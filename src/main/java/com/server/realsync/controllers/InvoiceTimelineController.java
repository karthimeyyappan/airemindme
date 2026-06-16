package com.server.realsync.controllers;

import com.server.realsync.dto.InvoiceTimelineDTO;
import com.server.realsync.services.InvoiceTimelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for invoice timeline events.
 *
 * Timeline events are stored as JSON inside the existing Invoice row —
 * no separate DB table is needed.
 *
 *   GET  /api/invoices/{id}/timeline   — list events (oldest first)
 *   POST /api/invoices/{id}/timeline   — append a new event
 *       Body: { "action": "whatsapp_sent", "note": "optional" }
 */
@RestController
@RequestMapping("/api/invoices/{id}/timeline")
public class InvoiceTimelineController {

    private final InvoiceTimelineService timelineService;

    public InvoiceTimelineController(InvoiceTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /** Returns all timeline events for the given invoice, oldest-first. */
    @GetMapping
    public ResponseEntity<List<InvoiceTimelineDTO>> getTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(timelineService.getTimeline(id));
    }

    /**
     * Appends a new timeline event.
     * Accepted actions: created, whatsapp_sent, email_sent, viewed,
     *                   reminder_sent, payment_received, cancelled
     */
    @PostMapping
    public ResponseEntity<InvoiceTimelineDTO> addEntry(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String action = body.get("action");
        if (action == null || action.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String note = body.getOrDefault("note", null);
        InvoiceTimelineDTO saved = timelineService.addEntry(id, action, note);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
