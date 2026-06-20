package com.server.realsync.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "referral_code", nullable = false, unique = true)
    private String referralCode;

    @Column(name = "referred_account_id")
    private Long referredAccountId;

    @Column(name = "referred_name")
    private String referredName;

    @Column(name = "referred_email")
    private String referredEmail;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "credit_given")
    private BigDecimal creditGiven = BigDecimal.ZERO;

    @Column(name = "referred_at")
    private LocalDateTime referredAt = LocalDateTime.now();

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    // ── Getters & Setters ──────────────────────────────────
    public Long getId() { return id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public Long getReferredAccountId() { return referredAccountId; }
    public void setReferredAccountId(Long referredAccountId) { this.referredAccountId = referredAccountId; }

    public String getReferredName() { return referredName; }
    public void setReferredName(String referredName) { this.referredName = referredName; }

    public String getReferredEmail() { return referredEmail; }
    public void setReferredEmail(String referredEmail) { this.referredEmail = referredEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCreditGiven() { return creditGiven; }
    public void setCreditGiven(BigDecimal creditGiven) { this.creditGiven = creditGiven; }

    public LocalDateTime getReferredAt() { return referredAt; }
    public void setReferredAt(LocalDateTime referredAt) { this.referredAt = referredAt; }

    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime creditedAt) { this.creditedAt = creditedAt; }
}