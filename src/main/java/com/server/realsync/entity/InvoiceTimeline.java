package com.server.realsync.entity;

import java.time.LocalDateTime;

/**
 * Plain POJO – NOT a JPA entity.
 *
 * Timeline events are serialised as a JSON array and stored inside the
 * {@code invoices.timeline_json} column on the existing {@link Invoice} table.
 * No separate DB table is created.
 *
 * Example JSON stored in Invoice.timelineJson:
 * [
 *   {"action":"created",        "note":null,           "createdAt":"2026-06-15T10:32:00"},
 *   {"action":"whatsapp_sent",  "note":"Sent to Raj",  "createdAt":"2026-06-15T10:35:00"}
 * ]
 */
public class InvoiceTimeline {

    /** E.g. created | whatsapp_sent | email_sent | viewed | reminder_sent | payment_received | cancelled */
    private String action;

    /** Optional descriptive note */
    private String note;

    /** ISO-8601 string – written when the entry is created */
    private LocalDateTime createdAt;

    public InvoiceTimeline() {
    }

    public InvoiceTimeline(String action, String note, LocalDateTime createdAt) {
        this.action    = action;
        this.note      = note;
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public String getAction()                  { return action;    }
    public void   setAction(String action)     { this.action = action; }

    public String getNote()                    { return note;      }
    public void   setNote(String note)         { this.note = note; }

    public LocalDateTime getCreatedAt()                      { return createdAt;    }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
