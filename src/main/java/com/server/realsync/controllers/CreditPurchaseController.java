package com.server.realsync.controllers;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.entity.Account;
import com.server.realsync.entity.AccountPlan;
import com.server.realsync.entity.CreditTransaction;
import com.server.realsync.entity.CustomUserDetails;
import com.server.realsync.repo.AccountPlanRepository;
import com.server.realsync.repo.CreditTransactionRepository;
import com.server.realsync.repo.TransactionRepository;
import com.server.realsync.services.AccountService;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/api/credits")
public class CreditPurchaseController {

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountService accountService;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseCredits(@RequestBody Map<String, Object> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int accountId = userDetails.getAccountId();
        
        Account account = accountService.findById(accountId).orElse(null);

        int amount = (int) body.get("amount");
        int credits = (int) body.get("credits");

        // Get active plan
        Optional<AccountPlan> planOpt = accountPlanRepository
            .findByAccountIdAndStatus(account.getId(), AccountPlan.PlanStatus.active);

        if (planOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "No active plan found"));
        }

        AccountPlan accountPlan = planOpt.get();

        // Create transaction record
        com.server.realsync.entity.Transaction tx = new com.server.realsync.entity.Transaction();
        tx.setAccount(account);
        tx.setPlan(accountPlan.getPlan());
        tx.setAmount(new java.math.BigDecimal(amount));
        tx.setCurrency("INR");
        tx.setPaymentStatus(com.server.realsync.entity.Transaction.PaymentStatus.success);
        tx.setPaymentMethod("dummy");
        tx.setGatewayOrderId("CREDIT-" + System.currentTimeMillis());
        tx.setGatewayPaymentId("CREDIT-PAY-" + System.currentTimeMillis());
        tx.setPaidAt(java.time.LocalDateTime.now());
        transactionRepository.save(tx);

        // Add credits to account_plan balance
        double newBalance = accountPlan.getBalance() + credits;
        double newTotal = accountPlan.getTotalCredits() + credits;
        accountPlan.setBalance(newBalance);
        accountPlan.setTotalCredits(newTotal);
        accountPlanRepository.save(accountPlan);

        // Log credit transaction
        CreditTransaction ct = new CreditTransaction();
        ct.setAccountId(account.getId());
        ct.setAccountPlanId(accountPlan.getId());
        ct.setType("CREDITS_PURCHASED");
        ct.setCredits((double) credits);
        ct.setBalanceAfter(newBalance);
        ct.setRemarks("Purchased " + credits + " credits for ₹" + amount);
        creditTransactionRepository.save(ct);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "creditsAdded", credits,
            "newBalance", newBalance,
            "message", credits + " credits added successfully"
        ));
    }
}
