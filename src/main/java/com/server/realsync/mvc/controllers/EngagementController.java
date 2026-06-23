package com.server.realsync.mvc.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.server.realsync.util.SecurityUtil;

import jakarta.transaction.Transactional;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.ExecutionStatus;
import com.server.realsync.entity.Reminder;

import com.server.realsync.repo.ScheduleEntryRepository;
import com.server.realsync.repo.ReminderRepository;
import com.server.realsync.entity.Customer;
import com.server.realsync.services.CustomerService;
import com.server.realsync.entity.Greeting;
import com.server.realsync.services.ReminderService;
import com.server.realsync.services.GreetingService;
import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.entity.ScheduleEntryStatus;

import com.server.realsync.services.FileStorageService;

@RestController
@RequestMapping("/api/engagements")
@CrossOrigin(origins = "*")
public class EngagementController {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreetingService greetingService;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private CustomerService customerService;

    // 1. REMINDER APIS

    @GetMapping("/reminders/account/{accountId}")
    public List<Reminder> getAllReminders(@PathVariable Integer accountId) {
        return reminderService.getByAccountId(accountId);
    }

    @GetMapping("/reminders/account/{accountId}/list")
    public ResponseEntity<?> getAllRemindersWithCustomer(
            @PathVariable Integer accountId) {

        List<Reminder> reminders = reminderService.getByAccountId(accountId);

        List<Map<String, Object>> result = reminders.stream()
                .map(r -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", r.getId());
                    map.put("title", r.getTitle());
                    map.put("message", r.getMessage());
                    map.put("reminderPurpose", r.getReminderPurpose());
                    map.put("reminderType", r.getReminderType());
                    map.put("recurring", r.getRecurring());
                    map.put("frequency", r.getFrequency());
                    map.put("totalOccurrences", r.getTotalOccurrences());
                    map.put("amount", r.getAmount());
                    map.put("reminderDate", r.getReminderDate());
                    map.put("reminderTime", r.getReminderTime());
                    map.put("channel", r.getChannel());
                    map.put("status", r.getStatus());
                    map.put("createdAt", r.getCreatedAt());
                    map.put("customerId", r.getCustomerId());

                    // Fetch customer details
                    if (r.getCustomerId() != null) {
                        customerService.getById(
                                accountId, r.getCustomerId())
                                .ifPresent(c -> {
                                    map.put("customerName", c.getName());
                                    map.put("customerMobile", c.getMobile());
                                });
                    }
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/reminders/{id}")
    public ResponseEntity<Reminder> getReminderById(@PathVariable Integer id, @RequestParam Integer accountId) {
        return reminderService.getById(id, accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reminders")
    public Reminder createReminder(@RequestBody Reminder reminder) {
        if (reminder.getAccountId() == null || reminder.getCustomerId() == null) {
            throw new RuntimeException("AccountId and CustomerId are required");
        }
        return reminderService.save(reminder);
    }

    @PutMapping("/reminders/{id}")
    public ResponseEntity<?> updateReminder(
            @PathVariable Integer id,
            @RequestBody Reminder updatedReminder) {

        Account account = SecurityUtil.getCurrentAccountId();

        Optional<Reminder> optionalReminder = reminderService.getById(id, account.getId());

        if (optionalReminder.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reminder not found"));
        }

        Reminder existing = optionalReminder.get();

        // Ignore these fields on update — locked after creation
        // existing.setTotalOccurrences(...) → DO NOT call
        // existing.setReminderType(...) → DO NOT call
        // existing.setFrequency(...) → DO NOT call
        // existing.setCustomerId(...) → DO NOT call
        // existing.setTitle(...) → DO NOT call
        // existing.setReminderPurpose(...) → DO NOT call
        // existing.setReminderDate(...) → DO NOT call

        // Only update these fields on PUT:
        existing.setMessage(updatedReminder.getMessage());
        existing.setReminderTime(updatedReminder.getReminderTime());
        existing.setAmount(updatedReminder.getAmount());
        existing.setChannel(updatedReminder.getChannel());
        existing.setStatus(updatedReminder.getStatus());
        // attached item can be updated
        existing.setAttachedItemId(updatedReminder.getAttachedItemId());
        existing.setAttachedItemType(updatedReminder.getAttachedItemType());

        Reminder saved = reminderRepository.save(existing);

        List<ScheduleEntry> allEntries = scheduleEntryRepository
                .findByReminderIdOrderByOccurrenceDateAsc(
                        existing.getId().longValue());

        for (ScheduleEntry entry : allEntries) {

            // RULE 1: Never touch COMPLETED entries
            if (entry.getStatus() == ScheduleEntryStatus.COMPLETED) {
                continue;
            }

            // RULE 2: Never touch entries where sentWhatsapp=true
            // (reminder already sent)
            if (Boolean.TRUE.equals(entry.getSentWhatsapp())) {
                continue;
            }

            // RULE 3: For pending entries — update allowed fields

            // Update message only
            if (updatedReminder.getMessage() != null) {
                entry.setMessageContent(updatedReminder.getMessage());
            }

            // Update time only (keep same date, just change hour)
            if (updatedReminder.getReminderTime() != null
                    && entry.getOccurrenceDate() != null) {
                entry.setOccurrenceDate(
                        entry.getOccurrenceDate()
                                .withHour(updatedReminder.getReminderTime().getHour())
                                .withMinute(0)
                                .withSecond(0));
            }

            // Update amount only
            // BUT: if entry has paidAmount > 0,
            // recalculate balance only — do NOT touch paidAmount
            if (updatedReminder.getAmount() != null) {
                java.math.BigDecimal newAmt = java.math.BigDecimal.valueOf(
                        updatedReminder.getAmount());
                entry.setAmount(newAmt);

                java.math.BigDecimal paid = entry.getPaidAmount();
                if (paid != null && paid.compareTo(
                        java.math.BigDecimal.ZERO) > 0) {
                    // Partially paid — recalculate balance only
                    java.math.BigDecimal newBal = newAmt.subtract(paid);
                    entry.setBalanceAmount(
                            newBal.compareTo(java.math.BigDecimal.ZERO) < 0
                                    ? java.math.BigDecimal.ZERO
                                    : newBal);
                }
                // If not paid yet — balance stays 0
                // (collected on payment)
            }

            scheduleEntryRepository.save(entry);
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        reminderService.delete(id, account.getId());
        return ResponseEntity.ok(Map.of("message", "Reminder deleted successfully"));
    }

    @GetMapping("/reminders/{id}/can-delete")
    public ResponseEntity<?> canDelete(@PathVariable Integer id) {

        Long reminderId = id.longValue();

        boolean hasPayments = reminderService.hasPaidEntries(reminderId);

        return ResponseEntity.ok(
                Map.of(
                        "canDelete", !hasPayments,
                        "hasPayments", hasPayments));
    }

    @GetMapping("/reminders/{id}/tracker")
    public List<ScheduleEntry> getTracker(@PathVariable Integer id) {
        return scheduleEntryRepository.findByReminderIdOrderByOccurrenceDateAsc(id.longValue());
    }

    @PostMapping("/schedule-entry/{id}/mark-paid")
    public ResponseEntity<?> markPaid(@PathVariable Long id) {

        ScheduleEntry entry = scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found"));

        entry.setStatus(ScheduleEntryStatus.COMPLETED);
        entry.setExecutionStatus(ExecutionStatus.SUCCESS);

        scheduleEntryRepository.save(entry);

        return ResponseEntity.ok(Map.of("message", "Marked as paid"));
    }

    @PostMapping("/schedule-entry/{id}/collect")
    @Transactional
    public ResponseEntity<?> collectPayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        try {
            ScheduleEntry entry = scheduleEntryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Entry not found"));

            double amountCollected = Double.parseDouble(
                    body.get("amountCollected").toString());
            String paymentMode = (String) body.get("paymentMode");
            String paymentDate = (String) body.get("paymentDate");
            String referenceNo = (String) body.getOrDefault("referenceNo", "");
            String notes = (String) body.getOrDefault("notes", "");

            double originalAmount = entry.getAmount() != null
                    ? entry.getAmount().doubleValue()
                    : 0;
            double alreadyPaid = entry.getPaidAmount() != null
                    ? entry.getPaidAmount().doubleValue()
                    : 0;

            double newTotalPaid = alreadyPaid + amountCollected;
            double newBalance = originalAmount - newTotalPaid;

            // Validation
            if (amountCollected <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Amount must be greater than 0"));
            }
            if (newTotalPaid > originalAmount) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Total paid ₹" + newTotalPaid +
                                        " exceeds amount ₹" + originalAmount));
            }

            // Find if this is last entry for this reminder
            List<ScheduleEntry> allEntries = scheduleEntryRepository
                    .findByReminderIdOrderByOccurrenceDateAsc(entry.getReminderId());
            boolean isLastEntry = allEntries.get(allEntries.size() - 1)
                    .getId().equals(id);

            // Last entry rule: must pay full amount
            if (isLastEntry && newBalance > 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Last installment — full amount required"));
            }

            // Update this entry
            entry.setPaidAmount(
                    java.math.BigDecimal.valueOf(newTotalPaid));
            entry.setBalanceAmount(
                    java.math.BigDecimal.valueOf(Math.max(0, newBalance)));
            entry.setPaymentMode(paymentMode);
            entry.setPaymentDate(java.time.LocalDate.parse(paymentDate));
            entry.setReferenceNo(referenceNo);
            entry.setPaymentNotes(notes);

            if (newBalance <= 0) {
                entry.setStatus(ScheduleEntryStatus.COMPLETED);
            } else {
                entry.setStatus(ScheduleEntryStatus.PENDING);
            }

            scheduleEntryRepository.save(entry);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "amountCollected", amountCollected,
                    "balance", Math.max(0, newBalance),
                    "status", entry.getStatus().name()));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/schedule-entry/{id}/message")
    public ResponseEntity<?> updateEntryMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ScheduleEntry entry = scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found"));
        entry.setMessageContent(body.get("message"));
        scheduleEntryRepository.save(entry);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/schedule-entry/{id}/send-now")
    public ResponseEntity<?> sendEntryNow(@PathVariable Long id) {
        // For now just mark sentWhatsapp = true and status = COMPLETED
        // Real send will be added later via QueueWorker
        ScheduleEntry entry = scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found"));
        entry.setSentWhatsapp(true);
        entry.setExecutionStatus(ExecutionStatus.SUCCESS);
        entry.setStatus(ScheduleEntryStatus.COMPLETED);
        scheduleEntryRepository.save(entry);
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Reminder sent successfully"));
    }

    @GetMapping("/reminders/{id}/history")
    public List<ScheduleEntry> getHistory(@PathVariable Long id) {
        return scheduleEntryRepository
                .findTop2ByReminderIdOrderByOccurrenceDateDesc(id);
    }

    @PostMapping("/reminders/{id}/cancel")
    public ResponseEntity<?> cancelReminder(@PathVariable Integer id) {

        Account account = SecurityUtil.getCurrentAccountId();

        reminderService.cancelRemainingInstallments(
                id,
                account.getId());

        return ResponseEntity.ok(
                Map.of("message", "Remaining installments removed"));
    }

    // 2. GREETING APIS (Added for completeness)

    @GetMapping("/greetings/account/{accountId}")
    public List<Greeting> getGreetings(@PathVariable Integer accountId) {
        return greetingService.getByAccountId(accountId);
    }

    @PostMapping("/greetings")
    public ResponseEntity<?> createGreeting(
            @RequestBody Greeting greeting) {

        Account account = SecurityUtil.getCurrentAccountId();

        greeting.setAccountId(account.getId());

        if (greeting.getStatus() == null) {
            greeting.setStatus("Scheduled");
        }

        Greeting saved = greetingService.save(greeting);

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", "created"));
    }

    @PutMapping("/greetings/{id}")
    public ResponseEntity<?> updateGreeting(
            @PathVariable Integer id,
            @RequestBody Greeting updatedGreeting) {

        try {

            Account account = SecurityUtil.getCurrentAccountId();

            Greeting existing = greetingService
                    .getById(id, account.getId())
                    .orElseThrow(() -> new RuntimeException("Greeting not found"));

            existing.setGreetingType(updatedGreeting.getGreetingType());

            existing.setCustomerId(updatedGreeting.getCustomerId());

            existing.setCustomerGroupId(updatedGreeting.getCustomerGroupId());

            existing.setMessage(updatedGreeting.getMessage());

            existing.setGreetingDate(updatedGreeting.getGreetingDate());

            existing.setGreetingTime(updatedGreeting.getGreetingTime());

            existing.setChannels(updatedGreeting.getChannels());

            existing.setStatus(updatedGreeting.getStatus());

            Greeting saved = greetingService.save(existing);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/greetings/{id}/image")
    public ResponseEntity<?> updateGreetingImage(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {

        try {
            System.out.println("UPDATE IMAGE API HIT");

            String imageUrl = body.get("imageUrl");

            Account account = SecurityUtil.getCurrentAccountId();

            Greeting greeting = greetingService.getById(id, account.getId())
                    .orElseThrow(() -> new RuntimeException("Greeting not found"));

            greeting.setImageUrl(imageUrl);

            greetingService.save(greeting);

            System.out.println("✅ Image saved to DB");

            return ResponseEntity.ok(Map.of("message", "Image updated"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/public/greeting/{greetingId}")
    public ResponseEntity<InputStreamResource> getPublicGreetingImage(
            @PathVariable Integer greetingId) {

        try {

            Greeting greeting = greetingService
                    .getById(greetingId, null)
                    .orElseThrow(() -> new RuntimeException("Greeting not found"));

            String imagePath = greeting.getImageUrl();

            int lastSlash = imagePath.lastIndexOf('/');

            String dir = imagePath.substring(0, lastSlash + 1);
            String fileName = imagePath.substring(lastSlash + 1);

            InputStream inputStream = fileStorageService.downloadFile(dir, fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                    .header(HttpHeaders.CACHE_CONTROL, "public,max-age=86400")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/greetings/{id}")
    public ResponseEntity<?> getGreetingById(
            @PathVariable Integer id) {

        try {

            System.out.println("GREETING API HIT");
            System.out.println("ID = " + id);

            Account account = SecurityUtil.getCurrentAccountId();

            System.out.println("ACCOUNT = " + account.getId());

            Greeting greeting = greetingService
                    .getById(id, account.getId())
                    .orElseThrow(() -> new RuntimeException("Greeting not found"));

            System.out.println("GREETING FOUND = " + greeting.getId());

            return ResponseEntity.ok(greeting);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/greetings/{id}")
    public ResponseEntity<?> deleteGreeting(@PathVariable Integer id, @RequestParam Integer accountId) {
        try {
            greetingService.delete(id, accountId);
            return ResponseEntity.ok(Map.of("message", "Greeting deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not delete greeting"));
        }
    }

    @GetMapping("/greetings/{id}/tracker")
    public ResponseEntity<?> getGreetingTracker(
            @PathVariable Integer id) {

        List<ScheduleEntry> trackers = greetingService.getGreetingEntries(id);

        List<Map<String, Object>> response = trackers.stream().map(t -> {

            Map<String, Object> map = new HashMap<>();

            map.put("id", t.getId());

            map.put("customerId", t.getCustomerId());

            Customer customer = customerService.getById(t.getCustomerId().intValue()).orElse(null);

            map.put("customerName", customer != null ? customer.getName() : "Unknown Customer");

            String channel = Boolean.TRUE.equals(t.getSentWhatsapp()) ? "wa"
                    : Boolean.TRUE.equals(t.getSentEmail())
                            ? "em"
                            : "sms";

            map.put("channel", channel);
            map.put("executionStatus", t.getExecutionStatus());
            map.put("occurrenceDate", t.getOccurrenceDate());
            map.put("sentWhatsapp", t.getSentWhatsapp());
            map.put("sentEmail", t.getSentEmail());

            return map;

        }).toList();

        return ResponseEntity.ok(response);
    }
    // 3. STATS APIS (Fixed Path Inconsistency)

    // GET /api/engagements/count/scheduled/1
    @GetMapping("/count/scheduled/{accountId}")
    public long countScheduled(@PathVariable Integer accountId) {
        return reminderService.countScheduledByAccountId(accountId);
    }

    // GET /api/engagements/count/sent-today/1
    @GetMapping("/count/sent-today/{accountId}")
    public long countSentToday(@PathVariable Integer accountId) {
        return reminderService.countSentToday(accountId);
    }

    // GET /api/engagements/stats/1
    @GetMapping("/stats/{accountId}")
    public ResponseEntity<?> getFullStats(@PathVariable Integer accountId) {
        try {
            // Calculate each stat safely
            long totalReminders = reminderService.getByAccountId(accountId).size();
            long totalGreetings = greetingService.getByAccountId(accountId).size();

            // Ensure these service methods return 0 instead of throwing errors if empty
            long scheduled = reminderService.countScheduledByAccountId(accountId);
            long sentToday = reminderService.countSentToday(accountId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("reminders", totalReminders);
            stats.put("greetings", totalGreetings);
            stats.put("scheduled", scheduled);
            stats.put("sentToday", sentToday);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // This will tell you EXACTLY what is failing in your IDE console
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error calculating stats: " + e.getMessage());
        }
    }

    @PostMapping("/reminders/{id}/send")
    public ResponseEntity<?> sendNow(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();

        return ResponseEntity.ok(Map.of("message", "Reminder sent successfully"));
    }

    @PostMapping("/reminders/{id}/reschedule")
    public ResponseEntity<?> reschedule(
            @PathVariable Integer id,
            @RequestParam String date,
            @RequestParam String time) {
        Account account = SecurityUtil.getCurrentAccountId();
        reminderService.reschedule(id, account.getId(), date, time);
        return ResponseEntity.ok(Map.of("message", "Rescheduled successfully"));
    }

    @PostMapping("/reminders/{id}/make-recurring")
    public ResponseEntity<?> makeRecurring(@PathVariable Integer id) {
        Account account = SecurityUtil.getCurrentAccountId();
        reminderService.makeRecurring(id, account.getId());
        return ResponseEntity.ok(Map.of("message", "Converted to recurring"));
    }

}