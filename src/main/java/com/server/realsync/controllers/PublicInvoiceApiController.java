package com.server.realsync.controllers;

import com.server.realsync.dto.PublicInvoiceDTO;
import com.server.realsync.services.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for public invoice operations.
 * Endpoints here are permitted without authentication (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/public/invoices")
public class PublicInvoiceApiController {

    private final InvoiceService invoiceService;

    public PublicInvoiceApiController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * GET /api/public/invoices/{token}
     * Returns the public invoice data for the given token.
     */
    @GetMapping("/{token}")
    public ResponseEntity<PublicInvoiceDTO> getByToken(@PathVariable String token) {
        PublicInvoiceDTO dto = invoiceService.getPublicInvoice(token);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/invoices/{id}/send
     * Sends the invoice as a WhatsApp message to the customer.
     * Protected (requires authentication).
     */
    @PostMapping("/send/{id}")
    public ResponseEntity<Map<String, String>> sendInvoice(@PathVariable Long id) {
        String result = invoiceService.sendInvoiceWhatsApp(id);
        return ResponseEntity.ok(Map.of("message", result));
    }
}
