package com.server.realsync.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.realsync.entity.Report;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.CatalogRTemplate;

import com.server.realsync.repo.ReportRepository;
import com.server.realsync.repo.CustomerRepository;
import com.server.realsync.repo.CatalogRTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.realsync.dto.ReportDashboardResponse;
import com.server.realsync.dto.ReportResponse;
import com.server.realsync.util.SecurityUtil;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ReportRepository repo;

    @Autowired
    private CustomerRepository customerRepo; // ✅ REQUIRED

    @Autowired
    private CatalogRTemplateRepository templateRepo; // ✅ REQUIRED

    // SAVE
    public Report save(Report report) {
        Account account = SecurityUtil.getCurrentAccountId();

        if (report.getId() == null) {
            report.setAccountId(account.getId());

            // Load template
            CatalogRTemplate template = null;
            if (report.getTemplateId() != null) {
                template = templateRepo.findById(report.getTemplateId()).orElse(null);
            }

            if (template != null) {
                report.setTemplateName(template.getTitle());
                report.setTemplateCategory(template.getCategory());
                report.setTemplatePrice(template.getPrice());

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> snapshot = new HashMap<>();
                    snapshot.put("id", template.getId());
                    snapshot.put("title", template.getTitle());
                    snapshot.put("category", template.getCategory());
                    snapshot.put("price", template.getPrice());
                    snapshot.put("fields", template.getParsedColumns());

                    report.setTemplateSnapshot(mapper.writeValueAsString(snapshot));
                } catch (Exception e) {
                    // Ignore mapping error
                }
            }

            // Generate reportNumber: RPT-YYYYMMDD-Random
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now());
            int randomVal = (int) (Math.random() * 900000) + 100000;
            report.setReportNumber("RPT-" + dateStr + "-" + randomVal);

            // Initialize reportData
            report.setReportData(report.getFields());

        } else {
            Report existing = repo.findById(report.getId()).orElse(null);
            if (existing != null) {
                if (!existing.getAccountId().equals(account.getId())) {
                    throw new RuntimeException("Unauthorized");
                }
                // Preserve metadata
                report.setAccountId(existing.getAccountId());
                report.setPatientId(existing.getPatientId());
                report.setTemplateId(existing.getTemplateId());
                report.setCreatedAt(existing.getCreatedAt());

                report.setReportNumber(existing.getReportNumber());
                report.setTemplateName(existing.getTemplateName());
                report.setTemplateCategory(existing.getTemplateCategory());
                report.setTemplatePrice(existing.getTemplatePrice());
                report.setTemplateSnapshot(existing.getTemplateSnapshot());

                // Update reportData (and fields for fallback)
                report.setReportData(report.getFields());
            }
        }

        return repo.save(report);
    }

    // OLD METHOD (keep if needed)
    public List<Report> getByAccountId(Integer accountId) {
        return repo.findByAccountId(accountId);
    }

    public Report getById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public List<ReportResponse> getAllReports() {

        Account account = SecurityUtil.getCurrentAccountId();

        List<Report> reports = repo.findByAccountId(account.getId());

        return reports.stream()
                .map(this::mapToResponse) // 🔥 reuse
                .toList();
    }

    public ReportDashboardResponse getDashboard() {
        Account account = SecurityUtil.getCurrentAccountId();
        List<Report> reports = repo.findByAccountIdOrderByCreatedAtDesc(account.getId());

        ReportDashboardResponse dashboard = new ReportDashboardResponse();
        dashboard.totalReports = reports.size();
        dashboard.paidReports = reports.stream().filter(this::isPaid).count();
        dashboard.sentReports = reports.stream().filter(this::isSent).count();
        dashboard.inProgressReports = reports.stream().filter(this::isInProgress).count();
        dashboard.unpaidReports = Math.max(0, dashboard.totalReports - dashboard.paidReports);

        dashboard.totalBilled = reports.stream().mapToDouble(this::priceOf).sum();
        dashboard.collectedAmount = reports.stream().filter(this::isPaid).mapToDouble(this::priceOf).sum();
        dashboard.outstandingAmount = Math.max(0, dashboard.totalBilled - dashboard.collectedAmount);

        dashboard.whatsappCount = dashboard.sentReports;
        dashboard.emailCount = 0;
        dashboard.smsCount = 0;

        Map<YearMonth, Long> volumeByMonth = reports.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getCreatedAt()), Collectors.counting()));

        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dashboard.monthlyVolume.add(new ReportDashboardResponse.MonthlyVolume(label,
                    volumeByMonth.getOrDefault(month, 0L)));
        }

        dashboard.topTests = reports.stream()
                .map(this::testNameOf)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    return byCount != 0 ? byCount : a.getKey().compareToIgnoreCase(b.getKey());
                })
                .limit(5)
                .map(e -> new ReportDashboardResponse.TopTest(e.getKey(), e.getValue()))
                .toList();

        dashboard.recentReports = reports.stream()
                .limit(5)
                .map(this::mapToRecentReport)
                .toList();

        return dashboard;
    }

    public ReportResponse getReportById(Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        Report r = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!r.getAccountId().equals(account.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        return mapToResponse(r);
    }

    private ReportResponse mapToResponse(Report r) {
        Customer c = customerRepo.findById(r.getPatientId()).orElse(null);

        ReportResponse res = new ReportResponse();

        res.id = r.getId();
        res.status = r.getStatus();
        res.date = r.getCreatedAt() != null ? r.getCreatedAt().toString() : "";
        res.setTemplateId(r.getTemplateId());
        res.setPatientId(r.getPatientId());
        res.reportNumber = r.getReportNumber();
        res.templateSnapshot = r.getTemplateSnapshot();
        res.reportData = r.getReportData();

        res.customerName = (c != null) ? c.getName() : "Unknown";
        res.mobile = (c != null) ? c.getMobile() : "";

        // Snapshot vs legacy fallback
        if (r.getTemplateSnapshot() != null && !r.getTemplateSnapshot().trim().isEmpty()) {
            res.reportName = r.getTemplateName();
            res.price = r.getTemplatePrice() != null ? r.getTemplatePrice() : 0.0;
            try {
                ObjectMapper mapper = new ObjectMapper();
                res.fields = mapper.readValue(r.getReportData(), List.class);
            } catch (Exception e) {
                res.fields = List.of();
            }
        } else {
            CatalogRTemplate t = null;
            if (r.getTemplateId() != null) {
                t = templateRepo.findById(r.getTemplateId()).orElse(null);
            }
            res.reportName = (t != null) ? t.getTitle() : "Manual Report";
            res.price = (t != null && t.getPrice() != null) ? t.getPrice() : 0.0;
            try {
                ObjectMapper mapper = new ObjectMapper();
                res.fields = mapper.readValue(r.getFields(), List.class);
            } catch (Exception e) {
                res.fields = List.of();
            }
        }

        return res;
    }

    private ReportDashboardResponse.RecentReport mapToRecentReport(Report r) {
        Customer c = r.getPatientId() != null ? customerRepo.findById(r.getPatientId()).orElse(null) : null;
        return new ReportDashboardResponse.RecentReport(
                r.getId(),
                r.getReportNumber(),
                testNameOf(r),
                c != null ? c.getName() : "Unknown",
                r.getStatus(),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : "",
                priceOf(r));
    }

    private boolean isPaid(Report r) {
        return "Completed".equalsIgnoreCase(statusOf(r));
    }

    private boolean isSent(Report r) {
        return "Shared".equalsIgnoreCase(statusOf(r)) || "Sent".equalsIgnoreCase(statusOf(r));
    }

    private boolean isInProgress(Report r) {
        String status = statusOf(r);
        return status == null || "Draft".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status);
    }

    private String statusOf(Report r) {
        return r != null ? r.getStatus() : null;
    }

    private double priceOf(Report r) {
        if (r == null) return 0.0;
        if (r.getTemplatePrice() != null) return r.getTemplatePrice();
        if (r.getTemplateId() == null) return 0.0;
        return templateRepo.findById(r.getTemplateId())
                .map(CatalogRTemplate::getPrice)
                .orElse(0.0);
    }

    private String testNameOf(Report r) {
        if (r == null) return "Manual Report";
        if (r.getTemplateName() != null && !r.getTemplateName().isBlank()) return r.getTemplateName();
        if (r.getTemplateId() == null) return "Manual Report";
        return templateRepo.findById(r.getTemplateId())
                .map(CatalogRTemplate::getTitle)
                .filter(Objects::nonNull)
                .filter(title -> !title.isBlank())
                .orElse("Manual Report");
    }

    // DELETE
    public void delete(Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        Report r = repo.findById(id).orElse(null);
        if (r != null && r.getAccountId().equals(account.getId())) {
            repo.delete(r);
        }
    }

   
}
