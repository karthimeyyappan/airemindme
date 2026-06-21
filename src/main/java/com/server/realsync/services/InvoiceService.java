package com.server.realsync.services;

import com.server.realsync.dto.*;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.CatalogProduct;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.InventoryTransaction;
import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoiceItem;
import com.server.realsync.entity.InvoiceStatus;
import com.server.realsync.mapper.InvoiceMapper;
import com.server.realsync.repo.AccountRepository;
import com.server.realsync.repo.CustomerRepository;
import com.server.realsync.repo.InvoiceRepository;
import com.server.realsync.spec.InvoiceSpecification;
import com.server.realsync.util.PublicTokenUtil;
import com.server.realsync.util.SecurityUtil;
import com.server.realsync.repo.InvoicePaymentRepository;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InventoryTransactionService txnService;
    @Autowired
    private CatalogProductService productService;
    @Autowired
    private RealSyncWhatsappService whatsappService;

    @Autowired
    private InvoiceTimelineService timelineService;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Value("${app.public.base-url:https://numen.uno}")
    private String publicBaseUrl;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    // ==========================
    // LIST / SEARCH (summary only)
    // ==========================
    public Page<InvoiceListResponseDTO> findAll(String search, Long customerId, String status,
            LocalDate dateFrom, LocalDate dateTo, Integer accountId, Pageable pageable) {
        Specification<Invoice> spec = InvoiceSpecification.filter(search, customerId, status, dateFrom, dateTo, accountId);

        return invoiceRepository.findAll(spec, pageable)
                .map(invoice -> {
                    InvoiceListResponseDTO dto = InvoiceMapper.toListDTO(invoice);
                    if (invoice.getCustomerId() != null) {
                        customerRepository.findById(invoice.getCustomerId().intValue())
                                .ifPresent(c -> dto.setCustomerName(c.getName()));
                    }
                    if ((dto.getCustomerName() == null || dto.getCustomerName().isBlank()) && invoice.getCustomerName() != null) {
                        dto.setCustomerName(invoice.getCustomerName());
                    }
                    return dto;
                });
    }

    public java.util.Map<String, Object> getSummary(Integer accountId, String period) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDate today = LocalDate.now();

        if ("today".equalsIgnoreCase(period)) {
            startDate = today;
            endDate = today;
        } else if ("weekly".equalsIgnoreCase(period)) {
            startDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            endDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } else if ("monthly".equalsIgnoreCase(period)) {
            startDate = today.withDayOfMonth(1);
            endDate = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        }

        java.math.BigDecimal totalOutstanding = invoiceRepository.getOutstandingBalance(accountId, startDate, endDate);
        java.math.BigDecimal overdue = invoiceRepository.getOverdueBalance(accountId, today, startDate, endDate);
        long overdueCount = invoiceRepository.getOverdueCount(accountId, today, startDate, endDate);
        java.math.BigDecimal dueToday = invoiceRepository.getDueTodayBalance(accountId, today, startDate, endDate);
        long dueTodayCount = invoiceRepository.getDueTodayCount(accountId, today, startDate, endDate);
        Double collectedToday = invoicePaymentRepository.getCollectedAmount(accountId, startDate, endDate);

        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalOutstanding", totalOutstanding);
        summary.put("overdue", overdue);
        summary.put("dueToday", dueToday);
        summary.put("collectedToday", collectedToday != null ? java.math.BigDecimal.valueOf(collectedToday) : java.math.BigDecimal.ZERO);
        summary.put("overdueCount", overdueCount);
        summary.put("dueTodayCount", dueTodayCount);

        return summary;
    }

    public Optional<Invoice> findEntityById(Long id) {
        return invoiceRepository.findById(id);
    }

    // ==========================
    // GET DETAIL
    // ==========================
    public InvoiceDetailResponseDTO getById(Long id) {
        return invoiceRepository.findById(id)
                .map(InvoiceMapper::toDetailDTO)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    // ==========================
    // CREATE
    // ==========================
    public InvoiceDetailResponseDTO create(CreateInvoiceRequestDTO req) {
        // ensure uniqueness of invoice number
        Optional<Invoice> existing = invoiceRepository.findByInvoiceNumber(req.getInvoiceNumber());
        if (existing.isPresent()) {
            throw new RuntimeException("Invoice number already exists");
        }

        Invoice invoice = InvoiceMapper.toEntity(req);
        invoice.setCustomerName(req.getCustomerName());
        invoice.setCustomerAddress(req.getCustomerAddress());
        invoice.setCustomerPhone(req.getCustomerPhone());
        invoice.setCustomerGst(req.getCustomerGst());
        invoice.setShippingAddress(req.getShippingAddress());
        Invoice saved = invoiceRepository.save(invoice);

        if (saved.getStatus() != InvoiceStatus.DRAFT
                && !Boolean.TRUE.equals(saved.getInventoryProcessed())) {

            processInventory(saved);

            saved.setInventoryProcessed(true);

            saved = invoiceRepository.save(saved);
        }

        // Auto-create the first timeline entry for every new invoice
        try {
            timelineService.addEntry(saved.getId(), "created");
        } catch (Exception ignore) {
            // Non-fatal: timeline write should never block invoice creation
        }

        return InvoiceMapper.toDetailDTO(saved);

    }

    // ==========================
    // UPDATE
    // ==========================
    public InvoiceDetailResponseDTO update(Long id, UpdateInvoiceRequestDTO dto) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id " + id));

        if (existing.getStatus() == InvoiceStatus.PARTIALLY_PAID
                || existing.getStatus() == InvoiceStatus.PAID
                || existing.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException("This invoice cannot be edited because payment transactions already exist.");
        }

        InvoiceMapper.updateEntityFromDto(existing, dto);
        existing.setCustomerName(dto.getCustomerName());
        existing.setCustomerAddress(dto.getCustomerAddress());
        existing.setCustomerPhone(dto.getCustomerPhone());
        existing.setCustomerGst(dto.getCustomerGst());
        existing.setShippingAddress(dto.getShippingAddress());
        Invoice saved = invoiceRepository.save(existing);

        if (saved.getStatus() != InvoiceStatus.DRAFT
                && !Boolean.TRUE.equals(saved.getInventoryProcessed())) {

            processInventory(saved);

            saved.setInventoryProcessed(true);

            saved = invoiceRepository.save(saved);
        }

        return InvoiceMapper.toDetailDTO(saved);
    }

    public InvoiceDetailResponseDTO cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id " + id));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Invoice is already cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            throw new RuntimeException("Paid or Partially Paid invoices cannot be cancelled");
        }

        // Restore inventory for PRODUCT items
        if (Boolean.TRUE.equals(invoice.getInventoryProcessed())) {
            for (InvoiceItem item : invoice.getItems()) {
                if ("PRODUCT".equalsIgnoreCase(item.getItemType()) && item.getItemRefId() != null) {
                    CatalogProduct product = productService.getById(
                            item.getItemRefId().intValue(),
                            SecurityUtil.getCurrentAccountId().getId()).orElse(null);
                    if (product != null) {
                        int currentQty = product.getQuantity() == null ? 0 : product.getQuantity();
                        int restoredQty = item.getQty() == null ? 0 : item.getQty();
                        int newQty = currentQty + restoredQty;
                        product.setQuantity(newQty);
                        productService.save(product);

                        // Create transaction
                        InventoryTransaction txn = new InventoryTransaction();
                        txn.setAccountId(product.getAccountId());
                        txn.setProductId(product.getId());
                        txn.setType("RESTOCK");
                        txn.setQuantity(restoredQty);
                        txn.setBalanceAfter(newQty);
                        txn.setReferenceNo(invoice.getInvoiceNumber());
                        txn.setNotes("Restored stock via cancelled Invoice " + invoice.getInvoiceNumber());
                        txnService.save(txn);
                    }
                }
            }
            invoice.setInventoryProcessed(false);
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);
        return InvoiceMapper.toDetailDTO(saved);
    }

    public Invoice save(Invoice invoice){
    return invoiceRepository.save(invoice);
}

    private void processInventory(Invoice invoice) {

        for (InvoiceItem item : invoice.getItems()) {

            if (!"PRODUCT".equalsIgnoreCase(item.getItemType())) {
                continue;
            }

            if (item.getItemRefId() == null) {
                continue;
            }

            CatalogProduct product = productService.getById(
                    item.getItemRefId().intValue(),
                    SecurityUtil.getCurrentAccountId().getId()).orElse(null);

            if (product == null) {
                continue;
            }

            int currentQty = product.getQuantity() == null
                    ? 0
                    : product.getQuantity();

            int soldQty = item.getQty() == null
                    ? 0
                    : item.getQty();

            if (soldQty > currentQty) {
                throw new RuntimeException(
                        "Insufficient stock for " + product.getName());
            }

            int newQty = currentQty - soldQty;

            product.setQuantity(newQty);

            productService.save(product);

            InventoryTransaction txn = new InventoryTransaction();

            txn.setAccountId(product.getAccountId());

            txn.setProductId(product.getId());

            txn.setType("SALE");

            txn.setQuantity(-soldQty);

            txn.setBalanceAfter(newQty);

            txn.setReferenceNo(
                    invoice.getInvoiceNumber());

            txn.setNotes(
                    "Sold via Invoice " +
                            invoice.getInvoiceNumber());

            txnService.save(txn);
        }
    }

    // ==========================
    // DELETE
    // ==========================
    public void delete(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id " + id));
        invoiceRepository.delete(invoice);
    }

    public List<Invoice> findAllByIds(Set<Long> ids) {
        return invoiceRepository.findAllById(ids);
    }

    // ==========================
    // FIND BY INVOICE NUMBER
    // ==========================
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    public String getNextInvoiceNumber() {
        String latest = invoiceRepository.findLatestInvoiceNumber();
        int currentYear = java.time.LocalDate.now().getYear();
        if (latest == null || latest.trim().isEmpty()) {
            return String.format("INV-%d-001", currentYear);
        }

        try {
            String[] parts = latest.split("-");
            if (parts.length == 3) {
                String prefix = parts[0];
                int year = Integer.parseInt(parts[1]);
                int nextSeq = Integer.parseInt(parts[2]) + 1;
                return String.format("%s-%d-%03d", prefix, currentYear, nextSeq);
            }
        } catch (Exception e) {
            // fallback
        }
        return String.format("INV-%d-001", currentYear);
    }

    // ==========================
    // PUBLIC INVOICE (token-based)
    // ==========================

    /**
     * Fetch invoice by public token (no auth required).
     * Enriches the DTO with business info from the Account.
     */
    public PublicInvoiceDTO getPublicInvoice(String token) {
        long invoiceId = PublicTokenUtil.decode(token);
        if (invoiceId < 0) {
            throw new RuntimeException("Invalid invoice token");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        PublicInvoiceDTO dto = new PublicInvoiceDTO();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerName(invoice.getCustomerName());
        dto.setCustomerAddress(invoice.getCustomerAddress());
        dto.setCustomerPhone(invoice.getCustomerPhone());
        dto.setCustomerGst(invoice.getCustomerGst());
        dto.setShippingAddress(invoice.getShippingAddress());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setDiscountAmount(invoice.getDiscountAmount());
        dto.setShippingAmount(invoice.getShippingAmount());
        dto.setGrandTotal(invoice.getGrandTotal());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setBalanceAmount(invoice.getBalanceAmount());
        dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        dto.setNotes(invoice.getNotes());
        dto.setTerms(invoice.getTerms());

        // Map items
        if (invoice.getItems() != null) {
            dto.setItems(invoice.getItems().stream().map(it -> {
                InvoiceItemDTO i = new InvoiceItemDTO();
                i.setId(it.getId());
                i.setItemType(it.getItemType());
                i.setItemRefId(it.getItemRefId());
                i.setItemName(it.getItemName());
                i.setDescription(it.getDescription());
                i.setHsnSac(it.getHsnSac());
                i.setQty(it.getQty());
                i.setRate(it.getRate());
                i.setGst(it.getGst());
                i.setTaxAmount(it.getTaxAmount());
                i.setLineTotal(it.getLineTotal());
                return i;
            }).collect(java.util.stream.Collectors.toList()));
        }

        // Enrich with business info via Customer -> Account
        if (invoice.getCustomerId() != null) {
            customerRepository.findById(invoice.getCustomerId().intValue()).ifPresent(customer -> {
                accountRepository.findById(customer.getAccountId()).ifPresent(account -> {
                    dto.setBusinessName(account.getBusinessName() != null ? account.getBusinessName() : account.getName());
                    dto.setBusinessPhone(account.getBusinessPhone() != null ? account.getBusinessPhone() : account.getMobile());
                    dto.setBusinessEmail(account.getBusinessEmail() != null ? account.getBusinessEmail() : account.getEmail());
                    dto.setBusinessAddress(account.getAddress());
                    dto.setBusinessGst(account.getGstNumber());
                });
            });
        }

        return dto;
    }

    // ==========================
    // SEND INVOICE VIA WHATSAPP
    // ==========================

    /**
     * Generate a public URL and send via MSG91 customer_document_ready template.
     */
    public String sendInvoiceWhatsApp(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String customerPhone = invoice.getCustomerPhone();
        if (customerPhone == null || customerPhone.isBlank()) {
            throw new RuntimeException("Customer phone number is not available on this invoice");
        }

        // Build token & public URL
        String token = PublicTokenUtil.encode(invoiceId);

        // Build slug from business name
        Account account = null;
        if (invoice.getCustomerId() != null) {
            Optional<Customer> optCustomer = customerRepository.findById(invoice.getCustomerId().intValue());
            if (optCustomer.isPresent()) {
                account = accountRepository.findById(optCustomer.get().getAccountId()).orElse(null);
            }
        }
        // Fallback: use current logged-in account
        if (account == null) {
            Integer currentAccountId = SecurityUtil.getCurrentAccountId().getId();
            if (currentAccountId != null && currentAccountId > 0) {
                account = accountRepository.findById(currentAccountId).orElse(null);
            }
        }

        String rawName = (account != null && account.getBusinessName() != null)
                ? account.getBusinessName() : (account != null ? account.getName() : "invoice");
        String slug = toSlug(rawName);

        String publicUrl = publicBaseUrl + "/" + slug + "/invoice/" + token;

        String businessName = (account != null && account.getBusinessName() != null)
                ? account.getBusinessName() : (account != null ? account.getName() : "");
        String businessPhone = (account != null && account.getBusinessPhone() != null)
                ? account.getBusinessPhone() : (account != null ? account.getMobile() : "");

        try {
            whatsappService.sendDocumentReadyTemplate(
                    customerPhone,
                    invoice.getCustomerName() != null ? invoice.getCustomerName() : "Customer",
                    invoice.getInvoiceNumber(),
                    publicUrl,
                    businessName,
                    businessPhone,
                    "Invoice"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WhatsApp message: " + e.getMessage(), e);
        }

        return "Invoice sent successfully via WhatsApp";
    }

    /** Convert business name to URL slug. E.g. "Muthu Pharmacy" -> "muthu-pharmacy" */
    private static String toSlug(String name) {
        if (name == null) return "shop";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}