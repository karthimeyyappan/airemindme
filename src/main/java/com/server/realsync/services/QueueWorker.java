package com.server.realsync.services;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.realsync.entity.*;
import kong.unirest.HttpResponse;
import org.json.JSONObject;

@Component
public class QueueWorker {

    private final MessageQueueService queueService;
    private final ScheduleEntryService scheduleEntryService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final RealSyncWhatsappService realSyncWhatsappService;
    private final ScheduleExecutionLogService logService;

    public QueueWorker(
            MessageQueueService queueService,
            ScheduleEntryService scheduleEntryService,
            CustomerService customerService,
            AccountService accountService,
            RealSyncWhatsappService realSyncWhatsappService,
            ScheduleExecutionLogService logService) {

        this.queueService = queueService;
        this.scheduleEntryService = scheduleEntryService;
        this.customerService = customerService;
        this.accountService = accountService;
        this.realSyncWhatsappService = realSyncWhatsappService;
        this.logService = logService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {

        List<MessageQueue> jobs = queueService.fetchJobs(50);

        for (MessageQueue job : jobs) {
            // Queue Safety: Only process if status is SENDING (or PENDING)
            if (job.getStatus() != QueueStatus.SENDING && job.getStatus() != QueueStatus.PENDING) {
                continue;
            }

            ScheduleEntry entry = null;
            Customer customer = null;
            String mobile = "N/A";
            String custIdStr = "N/A";

            try {
                if (job.getChannel() == QueueChannel.WHATSAPP && job.getEntityType() == EntityType.SCHEDULE) {

                    // Load ScheduleEntry
                    entry = scheduleEntryService.getById(job.getEntityEntryId()).orElse(null);
                    if (entry == null) {
                        throw new RuntimeException("ScheduleEntry not found for ID: " + job.getEntityEntryId());
                    }

                    // Load Customer
                    if (entry.getCustomerId() == null) {
                        throw new RuntimeException("CustomerId is null on ScheduleEntry ID: " + entry.getId());
                    }
                    custIdStr = String.valueOf(entry.getCustomerId());
                    customer = customerService.getById(entry.getCustomerId().intValue()).orElse(null);
                    if (customer == null) {
                        throw new RuntimeException("Customer not found for ID: " + entry.getCustomerId());
                    }

                    // Load Account
                    Account account = accountService.findById(customer.getAccountId()).orElse(null);
                    if (account == null) {
                        throw new RuntimeException("Account not found for ID: " + customer.getAccountId());
                    }

                    mobile = customer.getMobile();
                    if (mobile == null || mobile.isBlank()) {
                        throw new RuntimeException("Customer mobile number is missing");
                    }

                    String customerName = customer.getName();
                    String content = entry.getRemarks();
                    String businessName = account.getBusinessName() != null ? account.getBusinessName() : account.getName();
                    String businessMobile = account.getBusinessPhone() != null ? account.getBusinessPhone() : account.getMobile();

                    // Create log: SENDING
                    createExecutionLog(entry.getId(), ExecutionResult.SENDING, "WhatsApp delivery started");
                    System.out.println(String.format(
                            "WA_SENDING | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                            entry.getId(), custIdStr, mobile, job.getMessageId() != null ? job.getMessageId() : "N/A", job.getRetryCount(), job.getStatus()
                    ));

                    // Call MSG91 outbound API via RealSyncWhatsappService
                    HttpResponse<String> response = realSyncWhatsappService.sendReminderTemplate(
                            mobile,
                            customerName,
                            content,
                            businessName,
                            businessMobile
                    );

                    String responseBody = response.getBody();
                    String messageId = null;

                    if (response.getStatus() == 200 && responseBody != null) {
                        try {
                            JSONObject respJson = new JSONObject(responseBody);
                            if (respJson.has("request_id")) {
                                messageId = respJson.getString("request_id");
                            } else if (respJson.has("data")) {
                                JSONObject dataObj = respJson.getJSONObject("data");
                                if (dataObj.has("request_id")) {
                                    messageId = dataObj.getString("request_id");
                                }
                            }
                        } catch (Exception e) {
                            // Suppress JSON parsing errors, we still succeeded
                        }

                        // Update MessageQueue details
                        job.setStatus(QueueStatus.SENT);
                        job.setMessageId(messageId);
                        job.setSentAt(LocalDateTime.now());
                        queueService.save(job);

                        // Update ScheduleEntry details
                        entry.setSentWhatsapp(true);
                        entry.setExecutionStatus(ExecutionStatus.SUCCESS);
                        entry.setStatus(ScheduleEntryStatus.COMPLETED);
                        scheduleEntryService.save(entry);

                        // Create log: SENT
                        createExecutionLog(entry.getId(), ExecutionResult.SENT, "WhatsApp accepted by MSG91");

                        System.out.println(String.format(
                                "WA_SENT | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                                entry.getId(), custIdStr, mobile, messageId != null ? messageId : "N/A", job.getRetryCount(), "SENT"
                        ));
                    } else {
                        throw new RuntimeException("MSG91 API error status " + response.getStatus() + ": " + responseBody);
                    }
                } else {
                    // Mark other non-WhatsApp channels as done since we only enable WhatsApp delivery
                    queueService.markDone(job);
                }

            } catch (Exception ex) {
                int retryCount = job.getRetryCount() != null ? job.getRetryCount() : 0;
                retryCount++;
                job.setRetryCount(retryCount);
                job.setFailedReason(ex.getMessage());
                job.setUpdatedAt(LocalDateTime.now());

                if (retryCount < 3) {
                    // Temporary delivery failure -> retry
                    job.setStatus(QueueStatus.PENDING);
                    queueService.save(job);

                    if (entry != null) {
                        createExecutionLog(entry.getId(), ExecutionResult.RETRY, "Retry attempt " + retryCount + " of 3: " + ex.getMessage());
                        System.out.println(String.format(
                                "WA_RETRY | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                                entry.getId(), custIdStr, mobile, job.getMessageId() != null ? job.getMessageId() : "N/A", retryCount, "PENDING"
                        ));
                    }
                } else {
                    // Maximum retries reached -> FAILED
                    job.setStatus(QueueStatus.FAILED);
                    queueService.save(job);

                    if (entry != null) {
                        entry.setExecutionStatus(ExecutionStatus.FAILED);
                        scheduleEntryService.save(entry);

                        createExecutionLog(entry.getId(), ExecutionResult.FAILED, "Final failure: " + ex.getMessage());
                        System.out.println(String.format(
                                "WA_FAILED | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                                entry.getId(), custIdStr, mobile, job.getMessageId() != null ? job.getMessageId() : "N/A", retryCount, "FAILED"
                        ));
                    }
                }
            }
        }
    }

    private void createExecutionLog(Long scheduleEntryId, ExecutionResult status, String responseMsg) {
        try {
            ScheduleExecutionLog log = new ScheduleExecutionLog();
            log.setScheduleEntryId(scheduleEntryId);
            log.setChannel(Channel.WHATSAPP);
            log.setStatus(status);
            log.setResponse(responseMsg);
            logService.save(log);
        } catch (Exception e) {
            System.err.println("Failed to create execution log: " + e.getMessage());
        }
    }
}