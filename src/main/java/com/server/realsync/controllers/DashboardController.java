package com.server.realsync.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.entity.CustomUserDetails;
import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.entity.ScheduleEntryStatus;
import com.server.realsync.repo.AppointmentRepository;
import com.server.realsync.repo.ReminderRepository;
import com.server.realsync.repo.ScheduleEntryRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  @Autowired
  private ScheduleEntryRepository scheduleEntryRepository;

  @Autowired
  private AppointmentRepository appointmentRepository;

  @Autowired
  private ReminderRepository reminderRepository;

  @GetMapping("/summary")
  public ResponseEntity<?> getSummary() {

    Authentication authentication = SecurityContextHolder
      .getContext().getAuthentication();

    if (authentication == null ||
        !(authentication.getPrincipal() instanceof CustomUserDetails)) {
      return ResponseEntity.status(401)
        .body(Map.of("error", "Not logged in"));
    }

    CustomUserDetails userDetails =
      (CustomUserDetails) authentication.getPrincipal();
    Integer accountId = userDetails.getAccountId();

    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(23, 59, 59);

    List<ScheduleEntry> todayEntries =
      scheduleEntryRepository
        .findByAccountIdAndOccurrenceDateBetween(
          accountId, startOfDay, endOfDay);

    double collectToday = todayEntries.stream()
      .filter(e -> e.getStatus() != ScheduleEntryStatus.COMPLETED)
      .mapToDouble(e -> {
        double amt = e.getAmount() != null
          ? e.getAmount().doubleValue() : 0;
        double paid = e.getPaidAmount() != null
          ? e.getPaidAmount().doubleValue() : 0;
        return Math.max(0, amt - paid);
      })
      .sum();

    List<ScheduleEntry> overdueEntries =
      scheduleEntryRepository
        .findByAccountIdAndOccurrenceDateBeforeAndStatusNot(
          accountId, startOfDay,
          ScheduleEntryStatus.COMPLETED);

    double overdue = overdueEntries.stream()
      .mapToDouble(e -> {
        double amt = e.getAmount() != null
          ? e.getAmount().doubleValue() : 0;
        double paid = e.getPaidAmount() != null
          ? e.getPaidAmount().doubleValue() : 0;
        return Math.max(0, amt - paid);
      })
      .sum();

    long pendingTasks = scheduleEntryRepository
      .countByAccountIdAndStatus(
        accountId, ScheduleEntryStatus.PENDING);

    long appointments = appointmentRepository
      .countByAccountIdAndAppointmentDate(accountId, today);

    long remindersDue = reminderRepository
      .countByAccountIdAndReminderDate(accountId, today);

    long followups = reminderRepository
      .countByAccountIdAndReminderPurposeIn(
        accountId, List.of("followup", "service"));

    Map<String, Object> result = new HashMap<>();
    result.put("collectToday", collectToday);
    result.put("overdue", overdue);
    result.put("pendingTasks", pendingTasks);
    result.put("appointments", appointments);
    result.put("remindersDue", remindersDue);
    result.put("followups", followups);

    return ResponseEntity.ok(result);
  }
}