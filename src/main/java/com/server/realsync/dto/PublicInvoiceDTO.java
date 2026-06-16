package com.server.realsync.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only DTO for the public invoice view page.
 * Exposes no internal IDs – uses token for lookup.
 */
public class PublicInvoiceDTO {

    private String invoiceNumber;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String customerGst;
    private String shippingAddress;
    private LocalDate invoiceDate;
    private LocalDate dueDate;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;

    private String status;
    private String notes;
    private String terms;

    // Business info (from Account)
    private String businessName;
    private String businessPhone;
    private String businessEmail;
    private String businessAddress;
    private String businessGst;

    private List<InvoiceItemDTO> items;

    // ---- getters / setters ----

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String v) { this.invoiceNumber = v; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String v) { this.customerAddress = v; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { this.customerPhone = v; }

    public String getCustomerGst() { return customerGst; }
    public void setCustomerGst(String v) { this.customerGst = v; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { this.shippingAddress = v; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate v) { this.invoiceDate = v; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate v) { this.dueDate = v; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal v) { this.taxAmount = v; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }

    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal v) { this.shippingAmount = v; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal v) { this.grandTotal = v; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }

    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal v) { this.balanceAmount = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }

    public String getTerms() { return terms; }
    public void setTerms(String v) { this.terms = v; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String v) { this.businessName = v; }

    public String getBusinessPhone() { return businessPhone; }
    public void setBusinessPhone(String v) { this.businessPhone = v; }

    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String v) { this.businessEmail = v; }

    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String v) { this.businessAddress = v; }

    public String getBusinessGst() { return businessGst; }
    public void setBusinessGst(String v) { this.businessGst = v; }

    public List<InvoiceItemDTO> getItems() { return items; }
    public void setItems(List<InvoiceItemDTO> v) { this.items = v; }
}
