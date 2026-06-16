package com.server.realsync.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.realsync.dto.InvoiceTimelineDTO;
import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoiceTimeline;
import com.server.realsync.repo.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages invoice timeline events.
 *
 * Timeline events are stored as a JSON array in the {@code timeline_json}
 * column of the existing {@code invoices} table — no separate table is needed.
 *
 * ── One-time SQL migration ──────────────────────────────────────────────────
 *   ALTER TABLE invoices ADD COLUMN IF NOT EXISTS timeline_json MEDIUMTEXT NULL;
 * ───────────────────────────────────────────────────────────────────────────
 */
@Service
public class InvoiceTimelineService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Use the Spring-managed ObjectMapper (auto-configured by Spring Boot).
     * It already has JavaTimeModule registered, so LocalDateTime works correctly.
     */
    private final ObjectMapper objectMapper;

    public InvoiceTimelineService(InvoiceRepository invoiceRepository,
                                   ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.objectMapper = objectMapper;
    }

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Returns all timeline entries for an invoice (oldest first).
     */
    public List<InvoiceTimelineDTO> getTimeline(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        return parseEntries(invoice.getTimelineJson())
                .stream()
                .map(e -> toDTO(invoiceId, e))
                .collect(Collectors.toList());
    }

    /**
     * Appends a new timeline entry and persists the updated JSON.
     */
    public InvoiceTimelineDTO addEntry(Long invoiceId, String action) {
        return addEntry(invoiceId, action, null);
    }

    /**
     * Appends a new timeline entry with an optional note and persists.
     */
    public InvoiceTimelineDTO addEntry(Long invoiceId, String action, String note) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        List<InvoiceTimeline> entries = parseEntries(invoice.getTimelineJson());

        InvoiceTimeline newEntry = new InvoiceTimeline(action, note, LocalDateTime.now());
        entries.add(newEntry);

        invoice.setTimelineJson(serialise(entries));
        invoiceRepository.save(invoice);

        return toDTO(invoiceId, newEntry);
    }

    // ── Internal helpers ────────────────────────────────────────────

    private List<InvoiceTimeline> parseEntries(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<InvoiceTimeline>>() {});
        } catch (Exception e) {
            // Corrupt/old JSON — return empty so we never crash
            return new ArrayList<>();
        }
    }

    private String serialise(List<InvoiceTimeline> entries) {
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (Exception e) {
            return "[]";
        }
    }

    private InvoiceTimelineDTO toDTO(Long invoiceId, InvoiceTimeline e) {
        InvoiceTimelineDTO dto = new InvoiceTimelineDTO();
        dto.setInvoiceId(invoiceId);
        dto.setAction(e.getAction());
        dto.setNote(e.getNote());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
