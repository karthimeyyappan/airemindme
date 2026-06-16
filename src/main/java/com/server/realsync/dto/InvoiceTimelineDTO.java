package com.server.realsync.dto;

import java.time.LocalDateTime;

/**
 * Lightweight DTO returned by the timeline API.
 * Only the fields the frontend needs are exposed.
 */
public class InvoiceTimelineDTO {

    private Long id;
    private Long invoiceId;

    /** E.g. "created", "whatsapp_sent", "payment_received" */
    private String action;

    /** Optional descriptive note */
    private String note;

    private LocalDateTime createdAt;

    public InvoiceTimelineDTO() {
    }

    public InvoiceTimelineDTO(Long id, Long invoiceId, String action,
                               String note, LocalDateTime createdAt) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.action = action;
        this.note = note;
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
