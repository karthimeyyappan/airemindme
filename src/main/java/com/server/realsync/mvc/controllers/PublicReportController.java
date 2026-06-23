package com.server.realsync.mvc.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.server.realsync.entity.Report;
import com.server.realsync.entity.Customer;
import com.server.realsync.repo.ReportRepository;
import com.server.realsync.repo.CustomerRepository;

@Controller
public class PublicReportController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/r/{reportNumber}")
    public String publicReportPage(@PathVariable String reportNumber, Model model) {
        Report report = reportRepository.findByReportNumber(reportNumber);
        if (report == null) {
            return "redirect:/404";
        }

        // Fetch customer to populate transient fields for the view
        if (report.getPatientId() != null) {
            Customer c = customerRepository.findById(report.getPatientId()).orElse(null);
            if (c != null) {
                report.setCustomerName(c.getName());
                report.setMobile(c.getMobile());
            }
        }

        model.addAttribute("report", report);
        model.addAttribute("reportNumber", reportNumber);
        return "public-report";
    }
}
