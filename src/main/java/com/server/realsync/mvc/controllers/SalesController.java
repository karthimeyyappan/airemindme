package com.server.realsync.mvc.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.server.realsync.dto.InvoicePaymentListDTO;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoicePayment;
import com.server.realsync.entity.InvoiceStatus;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.InvoicePaymentService;
import com.server.realsync.services.InvoiceService;
import com.server.realsync.util.SecurityUtil;

@Controller
public class SalesController {

        @Autowired
        private AccountService accountService;

        @Autowired
        private InvoiceService invoiceService;

        @Autowired
        private InvoicePaymentService paymentService;

        @GetMapping("/sales.html")
        public String getSalesPage(Model model) {

                Account loggedIn = SecurityUtil.getCurrentAccountId();
                Account account = accountService.getById(loggedIn.getId());

                model.addAttribute("account", account);

                return "remindmeui/sales";
        }

        @GetMapping("/invoice-create.html")
        public String getInvoiceCreate(Model model) {

                Account loggedIn = SecurityUtil.getCurrentAccountId();
                Account account = accountService.getById(loggedIn.getId());

                model.addAttribute("account", account);
                model.addAttribute("activePage", "sales");

                return "remindmeui/invoice-create";
        }

        @GetMapping("/invoice-detail.html")
        public String getInvoiceDetail(
                        @RequestParam(required = false) Long id,
                        @RequestParam(required = false) Boolean preview,
                        Model model) {

                Account loggedIn = SecurityUtil.getCurrentAccountId();
                Account account = accountService.getById(loggedIn.getId());

                model.addAttribute("account", account);
                model.addAttribute("activePage", "sales");

                return "remindmeui/invoice-detail";
        }

        @GetMapping("/invoices/{id}/payments")
        public ResponseEntity<?> getPayments(
                        @PathVariable Integer id) {

                return ResponseEntity.ok(
                                paymentService.getByInvoice(id));
        }

  @PostMapping("/api/invoices/{id}/payments")
public ResponseEntity<?> recordPayment(@PathVariable Integer id, @RequestBody InvoicePayment payment) {
    try {
        Account account = SecurityUtil.getCurrentAccountId();
        Optional<Invoice> invoiceOpt = invoiceService.findEntityById(Long.valueOf(id));

        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invoice not found"));
        }

        // Basic Validations
        if (payment.getAmount() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Please enter payment amount."));
        }
        if (payment.getAmount() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Payment amount must be greater than zero."));
        }
        if (payment.getPaymentMode() == null || payment.getPaymentMode().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Payment mode is required"));
        }
        if (payment.getPaymentDate() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Please select payment date."));
        }
        if (payment.getPaymentDate().isAfter(java.time.LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Payment date cannot be in the future."));
        }

        Invoice inv = invoiceOpt.get();
        Double totalPaid = paymentService.getTotalPaid(id);
        Double currentPaid = (totalPaid == null) ? 0.0 : totalPaid;
        
        // Validation: Check if new payment exceeds remaining balance
        if ((currentPaid + payment.getAmount()) > inv.getGrandTotal().doubleValue()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Payment exceeds remaining invoice balance."));
        }

        // Save Payment
        payment.setAccountId(account.getId());
        payment.setInvoiceId(id);
        InvoicePayment saved = paymentService.save(payment);

        // Update Invoice Summary
        double newTotalPaid = currentPaid + payment.getAmount();
        inv.setPaidAmount(BigDecimal.valueOf(newTotalPaid));
        inv.setBalanceAmount(inv.getGrandTotal().subtract(inv.getPaidAmount()));

        // Update Status
        if (inv.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            inv.setStatus(InvoiceStatus.PAID);
        } else {
            inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceService.save(inv);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment recorded successfully",
                "paymentId", saved.getId()));

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", e.getMessage()));
    }
}

        @GetMapping("/api/payments/list")
        public ResponseEntity<?> getPaymentsList() {
                try {
                        Account account = SecurityUtil.getCurrentAccountId();
                        List<InvoicePayment> payments = paymentService.getByAccount(account.getId());

                        // Collect unique Invoice IDs to batch fetch
                        Set<Long> invoiceIds = payments.stream()
                                        .map(InvoicePayment::getInvoiceId)
                                        .filter(Objects::nonNull)
                                        .map(Long::valueOf)
                                        .collect(Collectors.toSet());

                        // Batch fetch invoices and map them by ID for O(1) lookup
                        Map<Long, Invoice> invoiceMap = invoiceService.findAllByIds(invoiceIds).stream()
                                        .collect(Collectors.toMap(Invoice::getId, invoice -> invoice));

                        List<InvoicePaymentListDTO> result = new ArrayList<>();

                        for (InvoicePayment payment : payments) {
                                InvoicePaymentListDTO dto = new InvoicePaymentListDTO();

                                // Populate basic payment fields
                                dto.setId(payment.getId());
                                dto.setAmount(payment.getAmount());
                                dto.setPaymentDate(payment.getPaymentDate());
                                dto.setPaymentMode(payment.getPaymentMode());
                                dto.setReferenceNo(payment.getReferenceNo());
                                dto.setNotes(payment.getNotes());
                                dto.setCreatedAt(payment.getCreatedAt());

                                // Populate invoice details from map
                                if (payment.getInvoiceId() != null) {
                                        Invoice invoice = invoiceMap.get(Long.valueOf(payment.getInvoiceId()));
                                        if (invoice != null) {
                                                dto.setInvoiceNumber(invoice.getInvoiceNumber());
                                                dto.setCustomerName(invoice.getCustomerName());
                                                dto.setInvoiceAmount(invoice.getGrandTotal() != null ? invoice.getGrandTotal().doubleValue() : 0.0);
                                                dto.setRemainingBalance(invoice.getBalanceAmount() != null ? invoice.getBalanceAmount().doubleValue() : 0.0);
                                                dto.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : "DRAFT");
                                                dto.setCreatedBy(invoice.getCreatedBy());
                                        }
                                }
                                result.add(dto);
                        }

                        return ResponseEntity.ok(result);

                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.internalServerError()
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

}