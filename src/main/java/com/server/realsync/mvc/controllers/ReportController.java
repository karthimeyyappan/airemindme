package com.server.realsync.mvc.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.Report;
import com.server.realsync.dto.ReportDashboardResponse;
import com.server.realsync.services.ReportService;
import com.server.realsync.util.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private com.server.realsync.repo.CustomerRepository customerRepository;

    @Autowired
    private com.server.realsync.repo.ReportRepository reportRepository;

    @Autowired
    private com.server.realsync.services.RealSyncWhatsappService realSyncWhatsappService;

    @GetMapping("/public/report/{reportNumber}")
    public ResponseEntity<?> getPublicReport(@PathVariable String reportNumber) {
        Report report = reportRepository.findByReportNumber(reportNumber);
        if (report == null) {
            return ResponseEntity.status(404).body("Report not found");
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{id}/share-whatsapp")
    public ResponseEntity<?> shareViaWhatsapp(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> payload) {
        try {
            Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

            report.setStatus("Shared");
            reportRepository.save(report);

            String mobile     = payload.get("mobile");
            String custName   = payload.get("customerName");
            String reportNum  = payload.get("reportNumber");
            String publicUrl  = payload.get("publicUrl");
            String tmplName   = payload.get("templateName");

            realSyncWhatsappService.sendDocumentReadyTemplate(
                mobile,
                custName,
                reportNum,
                publicUrl,
                tmplName,
                "",
                "Report"
            );

            return ResponseEntity.ok("Sent");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }

    // CREATE REPORT
    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody Report report) {

        Account account = SecurityUtil.getCurrentAccountId();

        report.setAccountId(account.getId());

        return ResponseEntity.ok(reportService.save(report));
    }

    // GET ALL REPORTS
    @GetMapping
    public List<Report> getReports() {

        Account account = SecurityUtil.getCurrentAccountId();

        return reportService.getByAccountId(account.getId());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ReportDashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }

    // GET SINGLE REPORT
    @GetMapping("/{id}")
    public ResponseEntity<?> getReport(@PathVariable Integer id) {

        Report report = reportService.getById(id);

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(report);
    }

    // SEARCH CUSTOMERS
    @GetMapping("/customers/search")
    public ResponseEntity<?> searchCustomers(@RequestParam String query) {

        Account account = SecurityUtil.getCurrentAccountId();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        return ResponseEntity.ok(
                customerRepository.searchByAccount(account.getId(), query, pageable)
                        .getContent()
                        .stream()
                        .map(c -> java.util.Map.of("id", c.getId(), "name", c.getName()))
                        .collect(java.util.stream.Collectors.toList()));
    }

    // DELETE REPORT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Integer id) {
        reportService.delete(id);
        return ResponseEntity.ok().build();
    }
}
