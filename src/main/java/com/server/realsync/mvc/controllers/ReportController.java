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

    @Autowired
    private com.server.realsync.repo.AccountPlanRepository accountPlanRepository;

    @Autowired
    private com.server.realsync.repo.CreditTransactionRepository creditTransactionRepository;

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
            Account account = SecurityUtil.getCurrentAccountId();

            // Credit check
            com.server.realsync.entity.AccountPlan plan = accountPlanRepository
                .findByAccountIdAndStatus(account.getId(),
                    com.server.realsync.entity.AccountPlan.PlanStatus.active)
                .orElse(null);

            if (plan == null || plan.getBalance() <= 0) {
                // Log failed credit transaction
                com.server.realsync.entity.CreditTransaction failCt =
                    new com.server.realsync.entity.CreditTransaction();
                failCt.setAccountId(account.getId());
                if (plan != null) failCt.setAccountPlanId(plan.getId());
                failCt.setType("WHATSAPP_FAILED_NO_CREDIT");
                failCt.setCredits(0.0);
                failCt.setBalanceAfter(plan != null ? plan.getBalance() : 0.0);
                failCt.setRemarks("REPORT share failed - no credits → " + payload.get("customerName"));
                creditTransactionRepository.save(failCt);

                return ResponseEntity.status(402).body(java.util.Map.of(
                    "success", false,
                    "code", "NO_CREDITS",
                    "message", "Insufficient WhatsApp credits"
                ));
            }

            // Deduct credit
            double newBal = plan.getBalance() - 1;
            plan.setBalance(newBal);
            accountPlanRepository.save(plan);

            com.server.realsync.entity.CreditTransaction ct =
                new com.server.realsync.entity.CreditTransaction();
            ct.setAccountId(account.getId());
            ct.setAccountPlanId(plan.getId());
            ct.setType("WHATSAPP_SENT");
            ct.setCredits(-1.0);
            ct.setBalanceAfter(newBal);
            ct.setRemarks("REPORT: " + payload.get("reportNumber") + " → " + payload.get("customerName"));
            creditTransactionRepository.save(ct);

            // Send WhatsApp
            Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

            report.setStatus("Shared");
            reportRepository.save(report);

            realSyncWhatsappService.sendDocumentReadyTemplate(
                payload.get("mobile"),
                payload.get("customerName"),
                payload.get("reportNumber"),
                payload.get("publicUrl"),
                payload.get("templateName"),
                "",
                "Report"
            );

            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "newBalance", newBal,
                "message", "Report sent successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                "success", false,
                "message", "Failed: " + e.getMessage()
            ));
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
