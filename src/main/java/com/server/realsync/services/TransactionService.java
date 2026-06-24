package com.server.realsync.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.Plan;
import com.server.realsync.entity.Transaction;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.PlanRepository;
import com.server.realsync.repo.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private AccountPlanRepository accountPlanRepo;

    @Autowired
    private PlanRepository planRepo;

    // Called when user initiates payment
    public Transaction createPendingTransaction(Account account, Plan plan, String currency) {
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setPlan(plan);
        tx.setCurrency(currency);
        tx.setAmount(getAmountForCurrency(plan, currency));
        tx.setPaymentStatus(Transaction.PaymentStatus.pending);
        return transactionRepo.save(tx);
    }

    // Called by payment gateway webhook on success
    @Transactional
    public void confirmPayment(String gatewayOrderId, String gatewayPaymentId, String paymentMethod) {
        Transaction tx = transactionRepo.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + gatewayOrderId));

        // Update transaction
        tx.setGatewayPaymentId(gatewayPaymentId);
        tx.setPaymentMethod(paymentMethod);
        tx.setPaymentStatus(Transaction.PaymentStatus.success);
        tx.setPaidAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        transactionRepo.save(tx);

        // Expire old active plan
        accountPlanRepo.findByAccountIdAndStatus(tx.getAccount().getId(), AccountPlan.PlanStatus.active)
                .ifPresent(old -> {
                    old.setStatus(AccountPlan.PlanStatus.expired);
                    accountPlanRepo.save(old);
                });

        // Create new account_plan
        Plan plan = tx.getPlan();
        AccountPlan ap = new AccountPlan();
        ap.setAccount(tx.getAccount());
        ap.setPlan(plan);
        ap.setTransaction(tx);
        ap.setStartDate(LocalDate.now());
        ap.setEndDate(calculateEndDate(plan));
        ap.setTotalCredits(Double.valueOf(plan.getWhatsappCredits()));
        ap.setBalance(plan.getWhatsappCredits());
        ap.setStatus(AccountPlan.PlanStatus.active);
        accountPlanRepo.save(ap);
    }

    private BigDecimal getAmountForCurrency(Plan plan, String currency) {
        return switch (currency) {

            case "USD" -> plan.getPriceUsd();

            default -> plan.getPriceInr();
        };
    }

    private LocalDate calculateEndDate(Plan plan) {
        return switch (plan.getBillingCycle()) {
            case half_yearly -> LocalDate.now().plusMonths(6);
            case yearly -> LocalDate.now().plusYears(1);
            case trial -> LocalDate.now().plusDays(plan.getTrialDays());
        };
    }
}
