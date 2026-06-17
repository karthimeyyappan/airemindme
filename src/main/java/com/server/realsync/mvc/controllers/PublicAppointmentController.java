package com.server.realsync.mvc.controllers;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.Appointment;
import com.server.realsync.entity.Customer;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

@Controller
public class PublicAppointmentController {

    private final AppointmentService appointmentService;
    private final AccountService accountService;

    public PublicAppointmentController(AppointmentService appointmentService, AccountService accountService) {
        this.appointmentService = appointmentService;
        this.accountService = accountService;
    }

    @GetMapping("/{businessSlug}/appointment/{publicToken}")
    public String viewPublicAppointment(
            @PathVariable String businessSlug,
            @PathVariable String publicToken,
            Model model) {

        Optional<Appointment> apptOpt = appointmentService.getByPublicToken(publicToken);
        if (apptOpt.isEmpty()) {
            model.addAttribute("errorTitle", "Appointment Not Found");
            model.addAttribute("errorMessage", "The appointment link is invalid or unavailable.");
            return "remindmeui/public-appointment-error";
        }

        Appointment appt = apptOpt.get();
        Account account = accountService.getById(appt.getAccountId());
        
        // Verify slug
        String rawName = (account.getBusinessName() != null) ? account.getBusinessName() : account.getName();
        String expectedSlug = toSlug(rawName);
        
        if (!expectedSlug.equalsIgnoreCase(businessSlug)) {
            model.addAttribute("errorTitle", "Appointment Not Found");
            model.addAttribute("errorMessage", "The appointment link is invalid or expired.");
            return "remindmeui/public-appointment-error";
        }

        model.addAttribute("appointment", appt);
        model.addAttribute("account", account);
        model.addAttribute("customer", appt.getCustomer() != null ? appt.getCustomer() : new Customer());
        model.addAttribute("businessSlug", businessSlug);

        return "remindmeui/public-appointment";
    }

    @PutMapping("/public/appointment/{publicToken}/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmAppointment(@PathVariable String publicToken) {
        Optional<Appointment> apptOpt = appointmentService.getByPublicToken(publicToken);
        if (apptOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Appointment not found"));
        }
        
        Appointment appt = apptOpt.get();
        appt.setStatus("CONFIRMED");
        appointmentService.update(appt, appt.getId(), appt.getAccountId());
        
        return ResponseEntity.ok(Map.of("message", "Appointment confirmed successfully"));
    }

    private static String toSlug(String name) {
        if (name == null) return "shop";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
