package com.server.realsync.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Appointment;
import com.server.realsync.entity.Channel;
import com.server.realsync.entity.CreditTransaction;
import com.server.realsync.entity.Customer;
import com.server.realsync.entity.EntityType;
import com.server.realsync.entity.ExecutionResult;
import com.server.realsync.entity.ExecutionStatus;
import com.server.realsync.entity.MessageQueue;
import com.server.realsync.entity.QueueChannel;
import com.server.realsync.entity.QueueStatus;
import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.entity.ScheduleEntryStatus;
import com.server.realsync.entity.ScheduleExecutionLog;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.CreditTransactionRepository;

import kong.unirest.HttpResponse;

@Component
public class QueueWorker {

    private final MessageQueueService queueService;
    private final ScheduleEntryService scheduleEntryService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final RealSyncWhatsappService realSyncWhatsappService;
    private final ScheduleExecutionLogService logService;

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private com.server.realsync.repo.GreetingRepository greetingRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Value("${app.public.base-url:https://numen.uno}")
    private String publicBaseUrl;

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
                    String content = entry.getMessageContent() != null && !entry.getMessageContent().isBlank()
                            ? entry.getMessageContent()
                            : entry.getRemarks();
                    String businessName = account.getBusinessName() != null ? account.getBusinessName()
                            : account.getName();
                    String businessMobile = account.getBusinessPhone() != null ? account.getBusinessPhone()
                            : account.getMobile();

                    if ("APPOINTMENT".equalsIgnoreCase(entry.getSourceType())) {
                        Appointment appt = appointmentService.getById(entry.getSourceId(), customer.getAccountId())
                                .orElse(null);
                        if (appt == null) {
                            throw new RuntimeException("Appointment not found for ID: " + entry.getSourceId());
                        }

                        String status = appt.getStatus();
                        if ("CANCELLED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                            entry.setStatus(ScheduleEntryStatus.CANCELLED);
                            entry.setExecutionStatus(ExecutionStatus.FAILED);
                            scheduleEntryService.save(entry);

                            job.setStatus(QueueStatus.FAILED);
                            job.setFailedReason("Appointment is " + status);
                            queueService.save(job);

                            createExecutionLog(entry.getId(), ExecutionResult.FAILED, "APPOINTMENT_REMINDER_CANCELLED");
                            System.out.println("APPOINTMENT_REMINDER_CANCELLED | scheduleEntryId=" + entry.getId()
                                    + " | reason=Appointment is " + status);
                            continue;
                        }

                        String rawName = (account != null && account.getBusinessName() != null)
                                ? account.getBusinessName()
                                : (account != null ? account.getName() : "business");
                        String slug = toSlug(rawName);
                        String publicUrl = publicBaseUrl + "/" + slug + "/appointment/" + appt.getPublicToken();

                        String dateStr = appt.getAppointmentDate() != null ? appt.getAppointmentDate().toString() : "";
                        String timeStr = appt.getAppointmentTime() != null ? appt.getAppointmentTime().toString() : "";

                        String dynamicContent = String.format(
                                "Reminder for your upcoming appointment.\n\nService: %s\nDate: %s\nTime: %s\n\nPlease confirm your appointment using:\n%s",
                                appt.getServiceName() != null ? appt.getServiceName() : "Appointment",
                                dateStr,
                                timeStr,
                                publicUrl);

                        createExecutionLog(entry.getId(), ExecutionResult.SENDING, "APPOINTMENT_REMINDER_QUEUED");
                        System.out.println("APPOINTMENT_REMINDER_QUEUED | scheduleEntryId=" + entry.getId());

                        // Credit check before sending WhatsApp
                        if (!deductWhatsAppCredit(account, entry, customer)) {
                            // No credits — mark as failed
                            job.setStatus(QueueStatus.FAILED);
                            job.setFailedReason("Insufficient WhatsApp credits");
                            queueService.save(job);

                            entry.setExecutionStatus(ExecutionStatus.FAILED);
                            scheduleEntryService.save(entry);

                            // Log failed credit transaction
                            CreditTransaction ct = new CreditTransaction();
                            ct.setAccountId(account.getId());
                            // Get active plan id if available
                            accountPlanRepository.findByAccountIdAndStatus(
                                    account.getId(), AccountPlan.PlanStatus.active)
                                    .ifPresent(ap -> ct.setAccountPlanId(ap.getId()));
                            ct.setType("WHATSAPP_FAILED_NO_CREDIT");
                            ct.setCredits(0.0);
                            ct.setBalanceAfter(0.0);
                            ct.setRemarks("Send failed - no credits for scheduleEntry #" + entry.getId());
                            creditTransactionRepository.save(ct);

                            createExecutionLog(entry.getId(), ExecutionResult.FAILED,
                                    "WhatsApp not sent - insufficient credits (balance=0)");
                            System.out.println("WA_BLOCKED_NO_CREDIT | scheduleEntryId=" + entry.getId()
                                    + " | accountId=" + account.getId());
                            continue;
                        }

                        HttpResponse<String> response = realSyncWhatsappService.sendReminderTemplate(
                                mobile,
                                customerName,
                                dynamicContent,
                                businessName,
                                businessMobile);

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
                                // Suppress JSON parsing
                            }

                            job.setStatus(QueueStatus.SENT);
                            job.setMessageId(messageId);
                            job.setSentAt(LocalDateTime.now());
                            queueService.save(job);

                            entry.setSentWhatsapp(true);
                            entry.setExecutionStatus(ExecutionStatus.SUCCESS);
                            entry.setStatus(ScheduleEntryStatus.COMPLETED);
                            scheduleEntryService.save(entry);

                            createExecutionLog(entry.getId(), ExecutionResult.SENT, "APPOINTMENT_REMINDER_SENT");
                            System.out.println("APPOINTMENT_REMINDER_SENT | scheduleEntryId=" + entry.getId());
                        } else {
                            throw new RuntimeException(
                                    "MSG91 API error status " + response.getStatus() + ": " + responseBody);
                        }
                    } else {
                        // Create log: SENDING
                        createExecutionLog(entry.getId(), ExecutionResult.SENDING, "WhatsApp delivery started");
                        System.out.println(String.format(
                                "WA_SENDING | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                                entry.getId(), custIdStr, mobile,
                                job.getMessageId() != null ? job.getMessageId() : "N/A", job.getRetryCount(),
                                job.getStatus()));

                        // Call MSG91 outbound API via RealSyncWhatsappService

                        // Credit check before sending WhatsApp
                        if (!deductWhatsAppCredit(account, entry, customer)) {
                            // No credits — mark as failed
                            job.setStatus(QueueStatus.FAILED);
                            job.setFailedReason("Insufficient WhatsApp credits");
                            queueService.save(job);

                            entry.setExecutionStatus(ExecutionStatus.FAILED);
                            scheduleEntryService.save(entry);

                            // Log failed credit transaction
                            CreditTransaction ct = new CreditTransaction();
                            ct.setAccountId(account.getId());
                            // Get active plan id if available
                            accountPlanRepository.findByAccountIdAndStatus(
                                    account.getId(), AccountPlan.PlanStatus.active)
                                    .ifPresent(ap -> ct.setAccountPlanId(ap.getId()));
                            ct.setType("WHATSAPP_FAILED_NO_CREDIT");
                            ct.setCredits(0.0);
                            ct.setBalanceAfter(0.0);
                            ct.setRemarks("Send failed - no credits for scheduleEntry #" + entry.getId());
                            creditTransactionRepository.save(ct);

                            createExecutionLog(entry.getId(), ExecutionResult.FAILED,
                                    "WhatsApp not sent - insufficient credits (balance=0)");
                            System.out.println("WA_BLOCKED_NO_CREDIT | scheduleEntryId=" + entry.getId()
                                    + " | accountId=" + account.getId());
                            continue;
                        }

                        HttpResponse<String> response;

                        // Check if GREETING with image
                        if ("GREETING".equalsIgnoreCase(entry.getSourceType()) && entry.getSourceId() != null) {
                            com.server.realsync.entity.Greeting greeting = greetingRepository.findById(entry.getSourceId().intValue()).orElse(null);
                            
                            if (greeting != null && greeting.getImageUrl() != null && !greeting.getImageUrl().isBlank()) {
                                // Build full public URL
                                String imagePublicUrl = publicBaseUrl + "/doc/view/greeting?path=" 
                                    + java.net.URLEncoder.encode(greeting.getImageUrl(), java.nio.charset.StandardCharsets.UTF_8);
                                
                                System.out.println("GREETING_WITH_IMAGE | url=" + imagePublicUrl);
                                
                                response = realSyncWhatsappService.sendReminderImageTemplate(
                                    mobile,
                                    customerName,
                                    content,
                                    businessName,
                                    businessMobile,
                                    imagePublicUrl
                                );
                            } else {
                                // Greeting without image — use normal template
                                response = realSyncWhatsappService.sendReminderTemplate(
                                    mobile, customerName, content, businessName, businessMobile
                                );
                            }
                        } else {
                            // Normal reminder — use normal template
                            response = realSyncWhatsappService.sendReminderTemplate(
                                mobile, customerName, content, businessName, businessMobile
                            );
                        }

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
                                    entry.getId(), custIdStr, mobile, messageId != null ? messageId : "N/A",
                                    job.getRetryCount(), "SENT"));
                        } else {
                            throw new RuntimeException(
                                    "MSG91 API error status " + response.getStatus() + ": " + responseBody);
                        }
                    }
                } else {
                    // Mark other non-WhatsApp channels as done since we only enable WhatsApp
                    // delivery
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
                        createExecutionLog(entry.getId(), ExecutionResult.RETRY,
                                "Retry attempt " + retryCount + " of 3: " + ex.getMessage());
                        System.out.println(String.format(
                                "WA_RETRY | scheduleEntryId=%d | customerId=%s | mobile=%s | messageId=%s | retryCount=%d | queueStatus=%s",
                                entry.getId(), custIdStr, mobile,
                                job.getMessageId() != null ? job.getMessageId() : "N/A", retryCount, "PENDING"));
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
                                entry.getId(), custIdStr, mobile,
                                job.getMessageId() != null ? job.getMessageId() : "N/A", retryCount, "FAILED"));
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

   private boolean deductWhatsAppCredit(Account account, ScheduleEntry entry, Customer customer) {
    try {
        // Get active plan
        Optional<AccountPlan> planOpt = accountPlanRepository
                .findByAccountIdAndStatus(account.getId(), AccountPlan.PlanStatus.active);

        if (planOpt.isEmpty()) {
            System.out.println("CREDIT_CHECK_FAILED | No active plan for accountId=" + account.getId());
            return false;
        }

        AccountPlan accountPlan = planOpt.get();

        // Check balance
        if (accountPlan.getBalance() <= 0) {
            System.out.println("CREDIT_EXHAUSTED | accountId=" + account.getId() + " | balance=0");
            return false;
        }

        // Deduct 1 credit
        double newBalance = accountPlan.getBalance() - 1;
        accountPlan.setBalance(newBalance);
        accountPlanRepository.save(accountPlan);

        // Build readable remarks
        String customerName = customer != null ? customer.getName() : "Customer";
        String sourceType = entry.getSourceType() != null ? entry.getSourceType() : "Message";
        String title = entry.getMessageContent() != null
                ? entry.getMessageContent().substring(0, Math.min(30, entry.getMessageContent().length()))
                : "Message";
        String dateStr = entry.getOccurrenceDate() != null
                ? entry.getOccurrenceDate().toLocalDate().toString()
                : "";

        // Log credit transaction
        CreditTransaction ct = new CreditTransaction();
        ct.setAccountId(account.getId());
        ct.setAccountPlanId(accountPlan.getId());
        ct.setType("WHATSAPP_SENT");
        ct.setCredits(-1.0);
        ct.setBalanceAfter(newBalance);
        ct.setRemarks(sourceType + ": " + title + " → " + customerName + " (" + dateStr + ")");
        creditTransactionRepository.save(ct);

        System.out.println("CREDIT_DEDUCTED | accountId=" + account.getId()
                + " | newBalance=" + newBalance);
        return true;

    } catch (Exception e) {
        System.err.println("Credit deduction failed: " + e.getMessage());
        return false;
    }
}

    private static String toSlug(String name) {
        if (name == null)
            return "shop";
        return name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}