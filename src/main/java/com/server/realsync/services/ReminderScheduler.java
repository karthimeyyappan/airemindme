package com.server.realsync.services;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.realsync.entity.EntityType;
import com.server.realsync.entity.MessageQueue;
import com.server.realsync.entity.QueueChannel;
import com.server.realsync.entity.ScheduleEntry;

/**
 * 
 */

@Component
public class ReminderScheduler {

    private final ScheduleEntryService scheduleEntryService;
    private final MessageQueueService queueService;
    private final ScheduleExecutionLogService logService;

    public ReminderScheduler(
            ScheduleEntryService scheduleEntryService,
            MessageQueueService queueService,
            ScheduleExecutionLogService logService) {

        this.scheduleEntryService = scheduleEntryService;
        this.queueService = queueService;
        this.logService = logService;
    }

    @Scheduled(fixedDelay = 10000)
    public void loadDueSchedules() {

        List<ScheduleEntry> entries =
                scheduleEntryService.getDueEntriesInWindow(java.time.LocalDateTime.now().plusHours(2));

        for (ScheduleEntry entry : entries) {
            if (queueService.exists(EntityType.SCHEDULE, entry.getId())) {
                continue;
            }

            MessageQueue job = new MessageQueue();

            job.setEntityType(EntityType.SCHEDULE);
            job.setEntityEntryId(entry.getId());
            job.setChannel(QueueChannel.WHATSAPP);

            int priority = 1;
            if ("GREETING".equalsIgnoreCase(entry.getSourceType())) {
                priority = 2;
            }
            job.setPriority(priority);

            queueService.save(job);

            // Create execution log for queued state
            try {
                com.server.realsync.entity.ScheduleExecutionLog log = new com.server.realsync.entity.ScheduleExecutionLog();
                log.setScheduleEntryId(entry.getId());
                log.setChannel(com.server.realsync.entity.Channel.WHATSAPP);
                log.setStatus(com.server.realsync.entity.ExecutionResult.QUEUED);
                
                String logResponse = "Reminder added to WhatsApp queue";
                if ("APPOINTMENT".equalsIgnoreCase(entry.getSourceType())) {
                    logResponse = "APPOINTMENT_REMINDER_QUEUED";
                    System.out.println("APPOINTMENT_REMINDER_QUEUED | scheduleEntryId=" + entry.getId());
                }
                log.setResponse(logResponse);
                logService.save(log);

                if (!"APPOINTMENT".equalsIgnoreCase(entry.getSourceType())) {
                    System.out.println(String.format(
                            "WA_QUEUE_CREATED | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=N/A | retryCount=0 | queueStatus=PENDING",
                            entry.getId(), entry.getCustomerId() != null ? String.valueOf(entry.getCustomerId()) : "N/A", "N/A"
                    ));
                }
            } catch (Exception e) {
                System.err.println("Failed to save execution log: " + e.getMessage());
            }
        }
    }
}