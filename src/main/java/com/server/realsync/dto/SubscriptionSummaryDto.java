package com.server.realsync.dto;

import com.server.realsync.entity.Plan;
import java.time.LocalDate;
import java.util.List;

public class SubscriptionSummaryDto {
    private Plan currentPlan;
    private String status;
    private LocalDate expiryDate;
    
    private Double totalCredits;
    private Double usedCredits;
    private Double remainingCredits;
    
    private Integer customerLimit;
    private Long customersUsed;
    private Long customersRemaining;
    
    private List<Plan> availablePlans;

    public Plan getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(Plan currentPlan) {
        this.currentPlan = currentPlan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(Double totalCredits) {
        this.totalCredits = totalCredits;
    }

    public Double getUsedCredits() {
        return usedCredits;
    }

    public void setUsedCredits(Double usedCredits) {
        this.usedCredits = usedCredits;
    }

    public Double getRemainingCredits() {
        return remainingCredits;
    }

    public void setRemainingCredits(Double remainingCredits) {
        this.remainingCredits = remainingCredits;
    }

    public Integer getCustomerLimit() {
        return customerLimit;
    }

    public void setCustomerLimit(Integer customerLimit) {
        this.customerLimit = customerLimit;
    }

    public Long getCustomersUsed() {
        return customersUsed;
    }

    public void setCustomersUsed(Long customersUsed) {
        this.customersUsed = customersUsed;
    }

    public Long getCustomersRemaining() {
        return customersRemaining;
    }

    public void setCustomersRemaining(Long customersRemaining) {
        this.customersRemaining = customersRemaining;
    }

    public List<Plan> getAvailablePlans() {
        return availablePlans;
    }

    public void setAvailablePlans(List<Plan> availablePlans) {
        this.availablePlans = availablePlans;
    }
}
