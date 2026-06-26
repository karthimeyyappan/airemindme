package com.server.realsync.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "referral_transaction")
public class ReferralTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "referrer_account_id", nullable = false)
    private Account referrerAccount;

    @ManyToOne
    @JoinColumn(name = "referred_account_id", nullable = false)
    private Account referredAccount;

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "purchase_amount", nullable = false)
    private BigDecimal purchaseAmount;

    @Column(name = "commission_percent", nullable = false)
    private BigDecimal commissionPercent;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public enum ReferralStatus {
        PENDING, CREDITED
    }

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Account getReferrerAccount() {
        return referrerAccount;
    }

    public void setReferrerAccount(Account referrerAccount) {
        this.referrerAccount = referrerAccount;
    }

    public Account getReferredAccount() {
        return referredAccount;
    }

    public void setReferredAccount(Account referredAccount) {
        this.referredAccount = referredAccount;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(BigDecimal purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
