package com.server.realsync.services;

import com.server.realsync.dto.SubscriptionSummaryDto;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Plan;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.PlanRepository;
import com.server.realsync.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import com.server.realsync.entity.Transaction;
import com.server.realsync.entity.ReferralTransaction;
import com.server.realsync.entity.CreditTransaction;
import com.server.realsync.repo.TransactionRepository;
import com.server.realsync.repo.ReferralTransactionRepository;
import com.server.realsync.repo.CreditTransactionRepository;
import com.server.realsync.repo.AccountRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReferralTransactionRepository referralTransactionRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public SubscriptionSummaryDto getSubscriptionSummary() {
        Account account = SecurityUtil.getCurrentAccountId();
        
        SubscriptionSummaryDto dto = new SubscriptionSummaryDto();
        
        Optional<AccountPlan> activePlanOpt = accountPlanRepository.findByAccountIdAndStatus(account.getId(), AccountPlan.PlanStatus.active);
        
        if (activePlanOpt.isPresent()) {
            AccountPlan accountPlan = activePlanOpt.get();
            Plan plan = accountPlan.getPlan();
            
            dto.setCurrentPlan(plan);
            dto.setExpiryDate(accountPlan.getEndDate());
            
            // Determine active/expired status based on endDate
            if (accountPlan.getEndDate() != null && accountPlan.getEndDate().isBefore(LocalDate.now())) {
                dto.setStatus("Expired");
            } else {
                dto.setStatus("Active");
            }
            
            // WhatsApp credits
            Double totalCredits = accountPlan.getTotalCredits() != null ? accountPlan.getTotalCredits() : 0.0;
            Double balance = accountPlan.getBalance();
            Double usedCredits = totalCredits - balance;
            
            dto.setTotalCredits(totalCredits);
            dto.setUsedCredits(usedCredits >= 0 ? usedCredits : 0);
            dto.setRemainingCredits(balance >= 0 ? balance : 0);
            
            // Customers Limit
            Integer limit = plan.getCustomerLimit();
            long usedCustomers = customerService.getTotalCustomers(account.getId());
            
            dto.setCustomerLimit(limit);
            dto.setCustomersUsed(usedCustomers);
            
            if (limit != null) {
                long remaining = limit - usedCustomers;
                dto.setCustomersRemaining(remaining >= 0 ? remaining : 0);
            } else {
                dto.setCustomersRemaining(null); // Unlimited
            }
            
        } else {
            // No active plan
            dto.setStatus("Expired");
            dto.setCurrentPlan(null);
            dto.setTotalCredits(0.0);
            dto.setUsedCredits(0.0);
            dto.setRemainingCredits(0.0);
            
            long usedCustomers = customerService.getTotalCustomers(account.getId());
            dto.setCustomersUsed(usedCustomers);
            dto.setCustomerLimit(0);
            dto.setCustomersRemaining(0L);
        }
        
        List<Plan> paidPlans = planRepository.findByIsActiveTrueAndIsTrialFalseOrderByPriceInrAsc();
        dto.setAvailablePlans(paidPlans);
        
        return dto;
    }

    @Transactional
    public Integer createPendingTransaction(Integer planId) {
        Account account = SecurityUtil.getCurrentAccountId();
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new RuntimeException("Plan not found"));

        Transaction txn = new Transaction();
        txn.setAccount(account);
        txn.setPlan(plan);
        txn.setAmount(plan.getPriceInr() != null ? plan.getPriceInr() : BigDecimal.ZERO);
        txn.setCurrency("INR");
        txn.setPaymentStatus(Transaction.PaymentStatus.pending);
        txn.setPaymentMethod("MOCK");

        Transaction savedTxn = transactionRepository.save(txn);
        return savedTxn.getId();
    }

    @Transactional
    public boolean completePurchase(Integer transactionId) {
        Transaction txn = transactionRepository.findById(transactionId).orElse(null);
        if (txn == null) return false;

        if (txn.getPaymentStatus() == Transaction.PaymentStatus.success) {
            return true; // Already processed
        }

        txn.setPaymentStatus(Transaction.PaymentStatus.success);
        txn.setPaidAt(LocalDateTime.now());
        txn.setGatewayPaymentId("MOCK_TXN_" + transactionId);
        transactionRepository.save(txn);

        Account account = txn.getAccount();
        Plan plan = txn.getPlan();

        // Load AccountPlan
        Optional<AccountPlan> activePlanOpt = accountPlanRepository.findByAccountIdAndStatus(account.getId(), AccountPlan.PlanStatus.active);
        AccountPlan accountPlan = activePlanOpt.orElse(new AccountPlan());

        if (accountPlan.getId() == null) {
            accountPlan.setAccount(account);
        }

        LocalDate baseDate = LocalDate.now();
        if (accountPlan.getEndDate() != null && accountPlan.getEndDate().isAfter(baseDate)) {
            baseDate = accountPlan.getEndDate();
        }

        LocalDate newEndDate;
        if (plan.getIsTrial()) {
            newEndDate = baseDate.plusDays(plan.getTrialDays() != null ? plan.getTrialDays() : 14);
        } else if (plan.getBillingCycle() == Plan.BillingCycle.half_yearly) {
            newEndDate = baseDate.plusMonths(6);
        } else {
            newEndDate = baseDate.plusMonths(12);
        }

        accountPlan.setPlan(plan);
        accountPlan.setStartDate(LocalDate.now());
        accountPlan.setEndDate(newEndDate);
        accountPlan.setTransaction(txn);
        accountPlan.setStatus(AccountPlan.PlanStatus.active);

        Double currentTotal = accountPlan.getTotalCredits() != null ? accountPlan.getTotalCredits() : 0.0;
        Double currentBalance = accountPlan.getBalance();

        Double planCredits = plan.getWhatsappCredits() != null ? plan.getWhatsappCredits().doubleValue() : 0.0;
        
        System.out.println("AccountPlan Balance Before: " + currentBalance);
        accountPlan.setTotalCredits(currentTotal + planCredits);
        accountPlan.setBalance(currentBalance + planCredits);
        System.out.println("AccountPlan Balance After: " + accountPlan.getBalance());

        accountPlan = accountPlanRepository.save(accountPlan);

        // Credit Transaction
        if (planCredits > 0) {
            CreditTransaction ct = new CreditTransaction();
            ct.setAccountId(account.getId());
            ct.setAccountPlanId(accountPlan.getId());
            ct.setType("PLAN_BOUGHT");
            ct.setCredits(planCredits);
            ct.setBalanceAfter(accountPlan.getBalance());
            ct.setRemarks(plan.getName() + " activated");
            creditTransactionRepository.save(ct);
            System.out.println("CreditTransaction Saved");
        }

        // Referral Logic
        System.out.println("Referral check... Buyer Account ID: " + account.getId() + " Buyer referredBy: " + account.getReferredBy() + " Transaction ID: " + txn.getId());
        if (account.getReferredBy() != null) {
            Account referrer = accountRepository.findById(account.getReferredBy()).orElse(null);
            if (referrer != null) {
                if (!referralTransactionRepository.existsByTransaction(txn)) {
                    BigDecimal purchaseAmount = txn.getAmount();
                    BigDecimal commissionPercent = new BigDecimal("5.00");
                    BigDecimal commissionAmount = purchaseAmount.multiply(commissionPercent).divide(new BigDecimal("100"));
                    System.out.println("Commission: " + commissionAmount);

                    ReferralTransaction refTxn = new ReferralTransaction();
                    refTxn.setReferrerAccount(referrer);
                    refTxn.setReferredAccount(account);
                    refTxn.setTransaction(txn);
                    refTxn.setPurchaseAmount(purchaseAmount);
                    refTxn.setCommissionPercent(commissionPercent);
                    refTxn.setCommissionAmount(commissionAmount);
                    refTxn.setStatus(ReferralTransaction.ReferralStatus.CREDITED);

                    referralTransactionRepository.save(refTxn);
                    System.out.println("ReferralTransaction Saved");

                    Double oldWallet = referrer.getWalletBalance() != null ? referrer.getWalletBalance() : 0.0;
                    System.out.println("Wallet Before: " + oldWallet);
                    
                    Double newWallet = oldWallet + commissionAmount.doubleValue();
                    referrer.setWalletBalance(newWallet);
                    
                    System.out.println("Wallet After: " + newWallet);
                    accountRepository.save(referrer);
                    System.out.println("Save Success");
                } else {
                    System.out.println("Referral skipped because existsByTransaction is true.");
                }
            } else {
                System.out.println("Referral skipped because referrer account not found.");
            }
        } else {
            System.out.println("Referral skipped because referredBy is null.");
        }

        return true;
    }
}
