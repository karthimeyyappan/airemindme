package com.server.realsync.dto;

import java.util.ArrayList;
import java.util.List;

public class ReportDashboardResponse {

    public long totalReports;
    public long paidReports;
    public long unpaidReports;
    public long sentReports;
    public long inProgressReports;

    public double totalBilled;
    public double collectedAmount;
    public double outstandingAmount;

    public long whatsappCount;
    public long emailCount;
    public long smsCount;

    public List<MonthlyVolume> monthlyVolume = new ArrayList<>();
    public List<TopTest> topTests = new ArrayList<>();
    public List<RecentReport> recentReports = new ArrayList<>();

    public static class MonthlyVolume {
        public String label;
        public long count;

        public MonthlyVolume(String label, long count) {
            this.label = label;
            this.count = count;
        }
    }

    public static class TopTest {
        public String name;
        public long count;

        public TopTest(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }

    public static class RecentReport {
        public Integer id;
        public String reportNumber;
        public String testName;
        public String customerName;
        public String status;
        public String date;
        public double amount;

        public RecentReport(Integer id, String reportNumber, String testName, String customerName,
                String status, String date, double amount) {
            this.id = id;
            this.reportNumber = reportNumber;
            this.testName = testName;
            this.customerName = customerName;
            this.status = status;
            this.date = date;
            this.amount = amount;
        }
    }
}
