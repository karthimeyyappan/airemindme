package com.server.realsync.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 
 */
@Entity
@Table(name = "promotion_entry")
public class PromotionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "triggered_date", nullable = false)
    private LocalDateTime triggeredDate;

    @Column(name = "sent_sms")
    private Boolean sentSms = false;

    @Column(name = "sent_email")
    private Boolean sentEmail = false;

    @Column(name = "sent_whatsapp")
    private Boolean sentWhatsapp = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "enquiry_count")
    private Integer enquiryCount = 0;

    @Column(name = "whatsapp_click_count")
    private Integer whatsappClickCount = 0;

    @Column(name = "phone_click_count")
    private Integer phoneClickCount = 0;

    @Column(name = "email_click_count")
    private Integer emailClickCount = 0;

    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "liked_at")
    private LocalDateTime likedAt;

    @Column(name = "enquiry_at")
    private LocalDateTime enquiryAt;

    @Column(name = "whatsapp_clicked_at")
    private LocalDateTime whatsappClickedAt;

    @Column(name = "phone_clicked_at")
    private LocalDateTime phoneClickedAt;

    @Column(name = "email_clicked_at")
    private LocalDateTime emailClickedAt;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public PromotionEntry() {}

    // Getters & Setters

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public Long getId() { return id; }

    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public LocalDateTime getTriggeredDate() { return triggeredDate; }
    public void setTriggeredDate(LocalDateTime triggeredDate) { this.triggeredDate = triggeredDate; }

    public Boolean getSentSms() { return sentSms; }
    public void setSentSms(Boolean sentSms) { this.sentSms = sentSms; }

    public Boolean getSentEmail() { return sentEmail; }
    public void setSentEmail(Boolean sentEmail) { this.sentEmail = sentEmail; }

    public Boolean getSentWhatsapp() { return sentWhatsapp; }
    public void setSentWhatsapp(Boolean sentWhatsapp) { this.sentWhatsapp = sentWhatsapp; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getEnquiryCount() { return enquiryCount; }
    public void setEnquiryCount(Integer enquiryCount) { this.enquiryCount = enquiryCount; }

    public Integer getWhatsappClickCount() { return whatsappClickCount; }
    public void setWhatsappClickCount(Integer whatsappClickCount) { this.whatsappClickCount = whatsappClickCount; }

    public Integer getPhoneClickCount() { return phoneClickCount; }
    public void setPhoneClickCount(Integer phoneClickCount) { this.phoneClickCount = phoneClickCount; }

    public Integer getEmailClickCount() { return emailClickCount; }
    public void setEmailClickCount(Integer emailClickCount) { this.emailClickCount = emailClickCount; }

    public LocalDateTime getFirstViewedAt() { return firstViewedAt; }
    public void setFirstViewedAt(LocalDateTime firstViewedAt) { this.firstViewedAt = firstViewedAt; }

    public LocalDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(LocalDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }

    public LocalDateTime getLikedAt() { return likedAt; }
    public void setLikedAt(LocalDateTime likedAt) { this.likedAt = likedAt; }

    public LocalDateTime getEnquiryAt() { return enquiryAt; }
    public void setEnquiryAt(LocalDateTime enquiryAt) { this.enquiryAt = enquiryAt; }

    public LocalDateTime getWhatsappClickedAt() { return whatsappClickedAt; }
    public void setWhatsappClickedAt(LocalDateTime whatsappClickedAt) { this.whatsappClickedAt = whatsappClickedAt; }

    public LocalDateTime getPhoneClickedAt() { return phoneClickedAt; }
    public void setPhoneClickedAt(LocalDateTime phoneClickedAt) { this.phoneClickedAt = phoneClickedAt; }

    public LocalDateTime getEmailClickedAt() { return emailClickedAt; }
    public void setEmailClickedAt(LocalDateTime emailClickedAt) { this.emailClickedAt = emailClickedAt; }
}