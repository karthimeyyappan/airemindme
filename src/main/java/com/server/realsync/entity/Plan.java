package com.server.realsync.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "whatsapp_credits", nullable = false)
    private Integer whatsappCredits;

    @Column(name = "customer_limit")
    private Integer customerLimit; // NULL = unlimited

    @Column(name = "is_trial", nullable = false)
    private Boolean isTrial = false;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "price_inr")
    private BigDecimal priceInr;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    public enum BillingCycle {
        trial,
        half_yearly,
        yearly
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    // ---------------- GETTERS & SETTERS ----------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getWhatsappCredits() {
        return whatsappCredits;
    }

    public void setWhatsappCredits(Integer whatsappCredits) {
        this.whatsappCredits = whatsappCredits;
    }

    public Integer getCustomerLimit() {
        return customerLimit;
    }

    public void setCustomerLimit(Integer customerLimit) {
        this.customerLimit = customerLimit;
    }

    public Boolean getIsTrial() {
        return isTrial;
    }

    public void setIsTrial(Boolean isTrial) {
        this.isTrial = isTrial;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        this.trialDays = trialDays;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public BigDecimal getPriceInr() {
        return priceInr;
    }

    public void setPriceInr(BigDecimal priceInr) {
        this.priceInr = priceInr;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public void setPriceUsd(BigDecimal priceUsd) {
        this.priceUsd = priceUsd;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}