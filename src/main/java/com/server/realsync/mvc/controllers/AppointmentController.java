package com.server.realsync.mvc.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.server.realsync.dto.AppointmentResponse;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.Appointment;
import com.server.realsync.services.AppointmentService;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AppointmentResponse> getAll() {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        return service.getAll(accountId);
    }

    @PostMapping
    public Appointment create(@RequestBody Appointment appt,
            @RequestParam Integer customerId) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        return service.create(appt, customerId, accountId);
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable Long id,
            @RequestBody Appointment appt) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        return service.update(appt, id, accountId);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        service.delete(id, accountId);
        return "Deleted";
    }

    @PostMapping("/{id}/status")
    public Appointment updateStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {

        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        return service.updateStatus(id, accountId, status, reason);
    }

    @GetMapping("/booked-slots")
    public List<java.util.Map<String, Object>> getBookedSlots(@RequestParam String date) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        List<Appointment> appts = service.getByDate(accountId, localDate);
        return appts.stream()
            .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
            .map(a -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("time", a.getAppointmentTime() != null ? a.getAppointmentTime().toString() : "");
                map.put("duration", a.getDurationMinutes());
                map.put("customerName", a.getCustomer() != null ? a.getCustomer().getName() : "Unknown");
                return map;
            }).toList();
    }
}