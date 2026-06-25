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
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    @Autowired
    private AccountPlanRepository accountPlanRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CustomerService customerService;

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
}
