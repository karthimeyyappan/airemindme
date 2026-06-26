package com.server.realsync.services;

import com.server.realsync.entity.*;
import com.server.realsync.repo.PromotionExecutionLogRepository;
import com.server.realsync.repo.PromotionItemRepository;
import com.server.realsync.repo.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PromotionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PromotionScheduler.class);

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionEntryService entryService;

    @Autowired
    private PromotionExecutionLogRepository logRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private com.server.realsync.repo.AccountRepository accountRepository;

    @Autowired
    private RealSyncWhatsappService realSyncWhatsappService;

    @Autowired
    private com.server.realsync.repo.AccountPlanRepository accountPlanRepository;

    @Autowired
    private com.server.realsync.repo.CreditTransactionRepository creditTransactionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void executeScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> scheduled = promotionRepository.findByStatusAndScheduledAtLessThanEqual("SCHEDULED", now);

        for (Promotion promo : scheduled) {
            try {
                // Prevent duplicate execution/race condition by changing status first
                promo.setStatus("ACTIVE");
                promotionRepository.save(promo);

                Account account = accountRepository.findById(promo.getAccountId()).orElse(null);
                if (account == null) {
                    logger.error("Account not found for scheduled promotion ID: {}", promo.getId());
                    continue;
                }

                List<PromotionEntry> entries = entryService.getByPromotion(promo.getId());
                if (entries.isEmpty() && promo.getCustomerGroupId() != null) {
                    // Create entries if not already created (for Group targeted promotions)
                    List<Customer> customers = customerService
                            .getByAccountAndGroup(promo.getAccountId(), promo.getCustomerGroupId(), Pageable.unpaged())
                            .getContent();

                    for (Customer c : customers) {
                        PromotionEntry entry = new PromotionEntry();
                        entry.setPromotionId(promo.getId());
                        entry.setCustomerId(c.getId());
                        entry.setTriggeredDate(LocalDateTime.now());
                        entry = entryService.save(entry);

                        PromotionExecutionLog log = new PromotionExecutionLog();
                        log.setPromotionEntryId(entry.getId());
                        log.setChannel(Channel.WHATSAPP); // Default channel
                        
                        sendPromotionWhatsApp(c, promo, account, entry, log);
                    }
                } else {
                    // Entries exist, ensure ExecutionLogs exist and send
                    for (PromotionEntry entry : entries) {
                        List<PromotionExecutionLog> logs = logRepository.findByPromotionEntryId(entry.getId());
                        if (logs.isEmpty()) {
                            PromotionExecutionLog log = new PromotionExecutionLog();
                            log.setPromotionEntryId(entry.getId());
                            log.setChannel(Channel.WHATSAPP); // Default channel
                            sendPromotionWhatsApp(getCustomerEntity(entry.getCustomerId(), promo.getAccountId()), promo, account, entry, log);
                        } else {
                            for (PromotionExecutionLog log : logs) {
                                if (log.getStatus() == ExecutionResult.PENDING && log.getChannel() == Channel.WHATSAPP) {
                                    sendPromotionWhatsApp(getCustomerEntity(entry.getCustomerId(), promo.getAccountId()), promo, account, entry, log);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Log exception but continue processing other scheduled promotions
                logger.error("Error executing scheduled promotion ID: {}", promo.getId(), e);
            }
        }
    }

    private void sendPromotionWhatsApp(Customer c, Promotion promo, Account account, PromotionEntry entry, PromotionExecutionLog log) {
        if (c == null) {
            log.setStatus(ExecutionResult.FAILED);
            String errMsg = "Customer entity is null";
            log.setResponse(errMsg);
            logRepository.save(log);
            
            entry.setStatus("FAILED");
            entry.setFailureReason(errMsg);
            entryService.save(entry);
            return;
        }
        String mobile = c.getMobile();
        if (mobile == null || mobile.isBlank()) {
            log.setStatus(ExecutionResult.FAILED);
            String errMsg = "Customer mobile number is missing";
            log.setResponse(errMsg);
            logRepository.save(log);
            
            entry.setStatus("FAILED");
            entry.setFailureReason(errMsg);
            entryService.save(entry);
            return;
        }

        try {
            String custName = c.getName() != null ? c.getName() : "Customer";
            
            // Defensive validations
            String busName = account.getBusinessName() != null ? account.getBusinessName().trim() : "";
            String busPhone = account.getBusinessPhone() != null ? account.getBusinessPhone().trim() : "";

            if (busName.isEmpty()) {
                throw new IllegalStateException("Business name is missing/blank for account ID " + account.getId());
            }
            if (busPhone.isEmpty()) {
                throw new IllegalStateException("Business phone is missing/blank for account ID " + account.getId());
            }

            String content = promo.getAiWhatsappContent();
            if (content == null || content.isBlank()) {
                content = promo.getDescription();
            }

            if (content != null && content.contains("localhost")) {
                throw new IllegalStateException("Promotion content contains invalid localhost URL");
            }
            if (promo.getPromotionUrl() != null && promo.getPromotionUrl().contains("localhost")) {
                throw new IllegalStateException("Promotion URL contains invalid localhost URL");
            }

            logger.info("SCHEDULER - SENDING WHATSAPP: mobile={}, customerName={}, businessName={}, businessMobile={}, content={}", 
                    mobile, custName, busName, busPhone, content);

            // Credit check
            com.server.realsync.entity.AccountPlan accountPlan = accountPlanRepository
                .findByAccountIdAndStatus(account.getId(), 
                    com.server.realsync.entity.AccountPlan.PlanStatus.active)
                .orElse(null);

            if (accountPlan == null || accountPlan.getBalance() <= 0) {
                log.setStatus(ExecutionResult.FAILED);
                String errMsg = "Insufficient WhatsApp credits";
                log.setResponse(errMsg);
                entry.setStatus("FAILED");
                entry.setFailureReason(errMsg);
                entryService.save(entry);
                logRepository.save(log);
                return;
            }

            // Deduct 1 credit
            double newBal = accountPlan.getBalance() - 1;
            accountPlan.setBalance(newBal);
            accountPlanRepository.save(accountPlan);

            com.server.realsync.entity.CreditTransaction ct = new com.server.realsync.entity.CreditTransaction();
            ct.setAccountId(account.getId());
            ct.setAccountPlanId(accountPlan.getId());
            ct.setType("WHATSAPP_SENT");
            ct.setCredits(-1.0);
            ct.setBalanceAfter(newBal);
            ct.setRemarks("SCHEDULED PROMO #" + promo.getId() + " → " + (c != null ? c.getName() : "Customer"));
            creditTransactionRepository.save(ct);

            kong.unirest.HttpResponse<String> response = realSyncWhatsappService.sendReminderTemplate(
                    mobile,
                    custName,
                    content,
                    busName,
                    busPhone
            );

            if (response.getStatus() == 200) {
                log.setStatus(ExecutionResult.SENT);
                log.setResponse("WhatsApp message sent successfully: " + response.getBody());
                entry.setSentWhatsapp(true);
                entry.setStatus("SENT");
                entry.setSentAt(LocalDateTime.now());
                entry.setFailureReason(null);
                entryService.save(entry);
            } else {
                log.setStatus(ExecutionResult.FAILED);
                String errMsg = "Failed to send: HTTP " + response.getStatus() + " - " + response.getBody();
                log.setResponse(errMsg);
                
                entry.setStatus("FAILED");
                entry.setFailureReason(errMsg);
                entryService.save(entry);
            }
        } catch (Exception ex) {
            logger.error("Scheduler failed to send WhatsApp message to {}", mobile, ex);
            log.setStatus(ExecutionResult.FAILED);
            String errMsg = "Failed to send: " + ex.getMessage();
            log.setResponse(errMsg);
            
            entry.setStatus("FAILED");
            entry.setFailureReason(errMsg);
            entryService.save(entry);
        }
        logRepository.save(log);
    }

    private Customer getCustomerEntity(Integer customerId, Integer accountId) {
        if (customerId == null) return null;
        return customerService.getById(accountId, customerId).orElse(null);
    }
}
