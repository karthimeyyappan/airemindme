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
