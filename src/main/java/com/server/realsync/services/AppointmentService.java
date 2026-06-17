package com.server.realsync.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.realsync.dto.AppointmentResponse;
import com.server.realsync.entity.Appointment;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.Account;
import com.server.realsync.repo.AppointmentRepository;
import com.server.realsync.repo.CustomerRepository;
import com.server.realsync.repo.AccountRepository;
import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.entity.ExecutionResult;
import com.server.realsync.repo.ScheduleEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RealSyncWhatsappService whatsappService;

    @Value("${app.public.base-url:https://numen.uno}")
    private String publicBaseUrl;

    @Autowired
    private ScheduleEntryRepository scheduleEntryRepository;

    @Autowired
    private ScheduleExecutionLogService logService;

    private void createLog(Long scheduleEntryId, ExecutionResult status, String response) {
        try {
            com.server.realsync.entity.ScheduleExecutionLog log = new com.server.realsync.entity.ScheduleExecutionLog();
            log.setScheduleEntryId(scheduleEntryId);
            log.setChannel(com.server.realsync.entity.Channel.WHATSAPP);
            log.setStatus(status);
            log.setResponse(response);
            logService.save(log);
        } catch (Exception e) {
            System.err.println("Failed to write ScheduleExecutionLog: " + e.getMessage());
        }
    }

    public AppointmentService(AppointmentRepository appointmentRepository,
            CustomerRepository customerRepository) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
    }

    public Appointment create(Appointment appointment, Integer customerId, Integer accountId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        appointment.setCustomer(customer);
        appointment.setAccountId(accountId);

        if (appointment.getStatus() == null) {
            appointment.setStatus("UPCOMING");
        }

        if (appointment.getPriority() == null || appointment.getPriority().isBlank()) {
            appointment.setPriority("MEDIUM");
        }

        // Overlap validation
        if (hasOverlap(accountId, appointment.getAppointmentDate(), appointment.getAppointmentTime(), appointment.getDurationMinutes(), null)) {
            throw new IllegalArgumentException("Overlapping appointment slot detected");
        }

        // Sequential Appointment Number generation
        String yearMonth = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String maxNum = appointmentRepository.findMaxAppointmentNumberForMonth(yearMonth);
        int nextSeq = 1;
        if (maxNum != null && maxNum.length() >= 15) {
            try {
                String seqStr = maxNum.substring(maxNum.lastIndexOf("-") + 1);
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        appointment.setAppointmentNumber(String.format("APT-%s-%04d", yearMonth, nextSeq));

        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setPublicToken(java.util.UUID.randomUUID().toString().replace("-", ""));

        Appointment saved = appointmentRepository.save(appointment);
        System.out.println("APPOINTMENT_CREATED | appointmentId=" + saved.getId());

        // Auto-send WhatsApp template
        try {
            Account account = accountRepository.findById(accountId).orElse(null);
            String rawName = (account != null && account.getBusinessName() != null)
                    ? account.getBusinessName() : (account != null ? account.getName() : "business");
            String slug = toSlug(rawName);
            String publicUrl = publicBaseUrl + "/" + slug + "/appointment/" + saved.getPublicToken();
            
            String businessName = (account != null && account.getBusinessName() != null)
                    ? account.getBusinessName() : (account != null ? account.getName() : "");
            
            String businessPhone = (account != null && account.getBusinessPhone() != null)
                    ? account.getBusinessPhone() : (account != null ? account.getMobile() : "");

            if (customer.getMobile() != null && !customer.getMobile().isBlank()) {
                whatsappService.sendAppointmentTemplate(
                        customer.getMobile(),
                        customer.getName() != null ? customer.getName() : "Customer",
                        "Appointment Confirmation",
                        saved.getAppointmentNumber(),
                        publicUrl,
                        businessName,
                        businessPhone
                );
                System.out.println("APPOINTMENT_CONFIRMATION_SENT | appointmentId=" + saved.getId());
            }
        } catch (Exception e) {
            System.err.println("Failed to send automatic WhatsApp confirmation: " + e.getMessage());
        }

        // Schedule Reminder Entry
        try {
            List<ScheduleEntry> existingEntries = scheduleEntryRepository.findBySourceTypeAndSourceId("APPOINTMENT", saved.getId());
            boolean activeExists = existingEntries.stream()
                    .anyMatch(e -> e.getStatus() != com.server.realsync.entity.ScheduleEntryStatus.CANCELLED);

            if (!activeExists) {
                LocalDateTime appointmentDateTime = LocalDateTime.of(saved.getAppointmentDate(), saved.getAppointmentTime());
                LocalDateTime calculatedTime = appointmentDateTime.minusHours(24);
                if (calculatedTime.isBefore(LocalDateTime.now())) {
                    calculatedTime = LocalDateTime.now();
                }

                ScheduleEntry entry = new ScheduleEntry();
                entry.setAccountId(accountId);
                entry.setCustomerId(customerId.longValue());
                entry.setSourceType("APPOINTMENT");
                entry.setSourceId(saved.getId());
                entry.setOccurrenceDate(calculatedTime);
                entry.setStatus(com.server.realsync.entity.ScheduleEntryStatus.PENDING);
                
                ScheduleEntry savedEntry = scheduleEntryRepository.save(entry);
                System.out.println("APPOINTMENT_REMINDER_SCHEDULED | appointmentId=" + saved.getId() + " | scheduleEntryId=" + savedEntry.getId());
                createLog(savedEntry.getId(), ExecutionResult.PENDING, "APPOINTMENT_REMINDER_SCHEDULED");
            }
        } catch (Exception e) {
            System.err.println("Failed to schedule appointment reminder: " + e.getMessage());
        }

        return saved;
    }

    public List<AppointmentResponse> getAll(Integer accountId) {

        List<Appointment> appointments = appointmentRepository.findAllWithCustomer(accountId);

        return appointments.stream().map(appt -> {

            AppointmentResponse dto = new AppointmentResponse();

            dto.setId(appt.getId());

            dto.setCustomerName(
                    appt.getCustomer() != null
                            ? appt.getCustomer().getName()
                            : "");
            dto.setCustomerId(
                    appt.getCustomer() != null
                            ? appt.getCustomer().getId()
                            : null);

            dto.setServiceName(appt.getServiceName());
            dto.setServiceType(appt.getServiceType());
            dto.setAssignee(appt.getAssignee());

            dto.setAppointmentDate(appt.getAppointmentDate());
            dto.setAppointmentTime(appt.getAppointmentTime());

            dto.setDurationMinutes(appt.getDurationMinutes());

            dto.setReminderType(appt.getReminderType());
            dto.setChannel(appt.getChannel());
            dto.setStatus(appt.getStatus());

            dto.setNotes(appt.getNotes());
            dto.setAppointmentNumber(appt.getAppointmentNumber());
            dto.setRequiredDocuments(appt.getRequiredDocuments());
            dto.setCancelReason(appt.getCancelReason());
            dto.setPriority(appt.getPriority());
            dto.setPublicToken(appt.getPublicToken());

            return dto;

        }).toList();
    }

    public Optional<Appointment> getById(Long id, Integer accountId) {
        return appointmentRepository.findByIdAndAccountId(id, accountId);
    }

    public Optional<Appointment> getByPublicToken(String token) {
        return appointmentRepository.findByPublicToken(token);
    }

    public Appointment update(Appointment updated, Long id, Integer accountId) {

        Appointment existing = getById(id, accountId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (hasOverlap(accountId, updated.getAppointmentDate(), updated.getAppointmentTime(), updated.getDurationMinutes(), id)) {
            throw new IllegalArgumentException("Overlapping appointment slot detected");
        }

        boolean dateOrTimeChanged = !existing.getAppointmentDate().equals(updated.getAppointmentDate())
                || !existing.getAppointmentTime().equals(updated.getAppointmentTime());

        existing.setServiceName(updated.getServiceName());
        existing.setServiceType(updated.getServiceType());
        existing.setAssignee(updated.getAssignee());

        existing.setAppointmentDate(updated.getAppointmentDate());
        existing.setAppointmentTime(updated.getAppointmentTime());

        existing.setDurationMinutes(updated.getDurationMinutes());
        existing.setReminderType(updated.getReminderType());
        existing.setChannel(updated.getChannel());
        existing.setNotes(updated.getNotes());
        existing.setRequiredDocuments(updated.getRequiredDocuments());
        existing.setPriority(updated.getPriority());

        existing.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(existing);

        if (dateOrTimeChanged) {
            try {
                List<ScheduleEntry> entries = scheduleEntryRepository.findBySourceTypeAndSourceId("APPOINTMENT", id);
                LocalDateTime newAppointmentDateTime = LocalDateTime.of(saved.getAppointmentDate(), saved.getAppointmentTime());
                LocalDateTime calculatedTime = newAppointmentDateTime.minusHours(24);
                if (calculatedTime.isBefore(LocalDateTime.now())) {
                    calculatedTime = LocalDateTime.now();
                }

                if (!entries.isEmpty()) {
                    ScheduleEntry entry = entries.get(0);
                    entry.setOccurrenceDate(calculatedTime);
                    entry.setStatus(com.server.realsync.entity.ScheduleEntryStatus.PENDING);
                    entry.setCustomerId(saved.getCustomer().getId().longValue());
                    entry.setAccountId(accountId);
                    scheduleEntryRepository.save(entry);

                    System.out.println("APPOINTMENT_REMINDER_SCHEDULED | appointmentId=" + id + " | scheduleEntryId=" + entry.getId() + " | updated_occurrenceDate=" + calculatedTime);
                    createLog(entry.getId(), ExecutionResult.PENDING, "APPOINTMENT_REMINDER_SCHEDULED (UPDATED)");
                } else {
                    ScheduleEntry entry = new ScheduleEntry();
                    entry.setAccountId(accountId);
                    entry.setCustomerId(saved.getCustomer().getId().longValue());
                    entry.setSourceType("APPOINTMENT");
                    entry.setSourceId(id);
                    entry.setOccurrenceDate(calculatedTime);
                    entry.setStatus(com.server.realsync.entity.ScheduleEntryStatus.PENDING);
                    
                    ScheduleEntry savedEntry = scheduleEntryRepository.save(entry);
                    System.out.println("APPOINTMENT_REMINDER_SCHEDULED | appointmentId=" + id + " | scheduleEntryId=" + savedEntry.getId());
                    createLog(savedEntry.getId(), ExecutionResult.PENDING, "APPOINTMENT_REMINDER_SCHEDULED");
                }
            } catch (Exception e) {
                System.err.println("Failed to update rescheduled appointment reminder: " + e.getMessage());
            }
        }

        return saved;
    }

    public void delete(Long id, Integer accountId) {

        Appointment appt = getById(id, accountId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appointmentRepository.delete(appt);
    }

    public Appointment updateStatus(Long id, Integer accountId, String status) {
        return updateStatus(id, accountId, status, null);
    }

    public Appointment updateStatus(Long id, Integer accountId, String status, String cancelReason) {

        Appointment appt = getById(id, accountId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        String oldStatus = appt.getStatus();
        String newStatus = status;

        if ("COMPLETED".equalsIgnoreCase(oldStatus) && "CANCELLED".equalsIgnoreCase(newStatus)) {
            throw new IllegalArgumentException("Cannot change status from COMPLETED to CANCELLED");
        }
        if ("CANCELLED".equalsIgnoreCase(oldStatus) && "COMPLETED".equalsIgnoreCase(newStatus)) {
            throw new IllegalArgumentException("Cannot change status from CANCELLED to COMPLETED");
        }

        appt.setStatus(status.toUpperCase());
        if ("CANCELLED".equalsIgnoreCase(status) && cancelReason != null) {
            appt.setCancelReason(cancelReason);
        }
        appt.setUpdatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appt);

        // Immediate notifications and reminder cancellation
        try {
            Customer customer = saved.getCustomer();
            Account account = accountRepository.findById(accountId).orElse(null);
            
            String customerName = (customer != null && customer.getName() != null) ? customer.getName() : "Customer";
            String customerMobile = (customer != null) ? customer.getMobile() : null;
            String businessName = (account != null && account.getBusinessName() != null) ? account.getBusinessName() : (account != null ? account.getName() : "");
            String businessMobile = (account != null && account.getBusinessPhone() != null) ? account.getBusinessPhone() : (account != null ? account.getMobile() : "");

            if ("CANCELLED".equalsIgnoreCase(status)) {
                if (customerMobile != null && !customerMobile.isBlank()) {
                    String dynamicContent = String.format(
                        "Your appointment has been cancelled.\nReason: %s\n\nPlease contact the business to reschedule.",
                        cancelReason != null ? cancelReason : "No reason specified"
                    );
                    whatsappService.sendReminderTemplate(
                        customerMobile,
                        customerName,
                        dynamicContent,
                        businessName,
                        businessMobile
                    );
                    System.out.println("APPOINTMENT_REMINDER_CANCELLED | appointmentId=" + saved.getId());
                }

                // Cancel pending ScheduleEntry records
                List<ScheduleEntry> pendingEntries = scheduleEntryRepository.findBySourceTypeAndSourceId("APPOINTMENT", id);
                for (ScheduleEntry entry : pendingEntries) {
                    if (entry.getStatus() == com.server.realsync.entity.ScheduleEntryStatus.PENDING) {
                        entry.setStatus(com.server.realsync.entity.ScheduleEntryStatus.CANCELLED);
                        scheduleEntryRepository.save(entry);
                        
                        System.out.println("APPOINTMENT_REMINDER_CANCELLED | scheduleEntryId=" + entry.getId());
                        createLog(entry.getId(), ExecutionResult.FAILED, "APPOINTMENT_REMINDER_CANCELLED");
                    }
                }
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                if (customerMobile != null && !customerMobile.isBlank()) {
                    String dynamicContent = String.format(
                        "Thank you for visiting! Your appointment for %s has been completed successfully. We appreciate your business and hope to see you again soon.",
                        saved.getServiceName() != null ? saved.getServiceName() : "your service"
                    );
                    whatsappService.sendReminderTemplate(
                        customerMobile,
                        customerName,
                        dynamicContent,
                        businessName,
                        businessMobile
                    );
                    System.out.println("APPOINTMENT_COMPLETED_MESSAGE_SENT | appointmentId=" + saved.getId());
                }

                // Cancel pending ScheduleEntry records
                List<ScheduleEntry> pendingEntries = scheduleEntryRepository.findBySourceTypeAndSourceId("APPOINTMENT", id);
                for (ScheduleEntry entry : pendingEntries) {
                    if (entry.getStatus() == com.server.realsync.entity.ScheduleEntryStatus.PENDING) {
                        entry.setStatus(com.server.realsync.entity.ScheduleEntryStatus.CANCELLED);
                        scheduleEntryRepository.save(entry);
                        
                        System.out.println("APPOINTMENT_REMINDER_CANCELLED | scheduleEntryId=" + entry.getId());
                        createLog(entry.getId(), ExecutionResult.FAILED, "APPOINTMENT_REMINDER_CANCELLED");
                        createLog(entry.getId(), ExecutionResult.SENT, "APPOINTMENT_COMPLETED_MESSAGE_SENT");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process appointment status transition notifications: " + e.getMessage());
        }

        return saved;
    }

    public boolean hasOverlap(Integer accountId, LocalDate date, LocalTime time, Integer duration, Long excludeId) {
        if (date == null || time == null || duration == null) {
            return false;
        }
        List<Appointment> existing = appointmentRepository.findByAccountIdAndAppointmentDate(accountId, date);
        int newStart = time.getHour() * 60 + time.getMinute();
        int newEnd = newStart + duration;

        for (Appointment appt : existing) {
            if (excludeId != null && appt.getId().equals(excludeId)) {
                continue;
            }
            if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
                continue;
            }
            if (appt.getAppointmentTime() == null || appt.getDurationMinutes() == null) {
                continue;
            }
            int start = appt.getAppointmentTime().getHour() * 60 + appt.getAppointmentTime().getMinute();
            int end = start + appt.getDurationMinutes();
            if (newStart < end && newEnd > start) {
                return true;
            }
        }
        return false;
    }

    public List<Appointment> getByDate(Integer accountId, LocalDate date) {
        return appointmentRepository.findByAccountIdAndAppointmentDate(accountId, date);
    }

    public List<Appointment> getToday(Integer accountId) {
        return appointmentRepository.findByAccountIdAndAppointmentDate(
                accountId, LocalDate.now());
    }

    public List<Appointment> getUpcoming(Integer accountId) {
        return appointmentRepository
                .findByAccountIdAndAppointmentDateGreaterThanEqual(
                        accountId, LocalDate.now());
    }

    public List<LocalDate> findAppointmentDatesForActivityChart(Integer accountId, LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findAppointmentDatesForActivityChart(accountId, startDate, endDate);
    }

    private static String toSlug(String name) {
        if (name == null) return "shop";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}