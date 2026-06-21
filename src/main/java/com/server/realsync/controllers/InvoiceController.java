package com.server.realsync.controllers;

import com.server.realsync.dto.*;
import java.time.LocalDate;
import com.server.realsync.entity.InvoiceStatus;
import com.server.realsync.services.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/next-number")
    public ResponseEntity<Map<String, String>> getNextInvoiceNumber() {
        String nextNum = invoiceService.getNextInvoiceNumber();
        Map<String, String> res = new HashMap<>();
        res.put("nextNumber", nextNum);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(defaultValue = "overall") String period) {
        Integer accountId = com.server.realsync.util.SecurityUtil.getCurrentAccountId().getId();
        return ResponseEntity.ok(invoiceService.getSummary(accountId, period));
    }

    @GetMapping
    public Page<InvoiceListResponseDTO> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Integer accountId = com.server.realsync.util.SecurityUtil.getCurrentAccountId().getId();
        return invoiceService.findAll(search, customerId, status, dateFrom, dateTo, accountId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/{id}")
    public InvoiceDetailResponseDTO getById(@PathVariable Long id) {
        return invoiceService.getById(id);
    }

    @PostMapping
    public ResponseEntity<InvoiceDetailResponseDTO> create(@Valid @RequestBody CreateInvoiceRequestDTO req) {
        InvoiceDetailResponseDTO saved = invoiceService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public InvoiceDetailResponseDTO update(@PathVariable Long id, @Valid @RequestBody UpdateInvoiceRequestDTO req) {
        return invoiceService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceDetailResponseDTO> cancel(@PathVariable Long id) {
        InvoiceDetailResponseDTO cancelled = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(cancelled);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Map<String, String>> sendInvoice(@PathVariable Long id) {
        String result = invoiceService.sendInvoiceWhatsApp(id);
        return ResponseEntity.ok(Map.of("message", result));
    }
}