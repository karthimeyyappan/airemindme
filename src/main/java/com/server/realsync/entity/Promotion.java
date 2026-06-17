/**
 * 
 */
package com.server.realsync.entity;

import java.time.LocalDateTime;

/**
 * 
 */
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "customer_group_id", nullable = true)
    private Integer customerGroupId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_generated_title")
    private String aiGeneratedTitle;

    @Column(name = "ai_whatsapp_content", length = 2000)
    private String aiWhatsappContent;

    @Column(name = "ai_blog_content", length = 5000)
    private String aiBlogContent;

    @Column(nullable = false)
    private String type;

    private String status;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_variant")
    private String templateVariant;

    @Column(name = "promotion_url", length = 1000)
    private String promotionUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Promotion() {
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getCustomerGroupId() {
        return customerGroupId;
    }

    public void setCustomerGroupId(Integer customerGroupId) {
        this.customerGroupId = customerGroupId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAiGeneratedTitle() {
        return aiGeneratedTitle;
    }

    public void setAiGeneratedTitle(String aiGeneratedTitle) {
        this.aiGeneratedTitle = aiGeneratedTitle;
    }

    public String getAiWhatsappContent() {
        return aiWhatsappContent;
    }

    public void setAiWhatsappContent(String aiWhatsappContent) {
        this.aiWhatsappContent = aiWhatsappContent;
    }

    public String getAiBlogContent() {
        return aiBlogContent;
    }

    public void setAiBlogContent(String aiBlogContent) {
        this.aiBlogContent = aiBlogContent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateVariant() {
        return templateVariant;
    }

    public void setTemplateVariant(String templateVariant) {
        this.templateVariant = templateVariant;
    }

    public String getPromotionUrl() {
        return promotionUrl;
    }

    public void setPromotionUrl(String promotionUrl) {
        this.promotionUrl = promotionUrl;
    }
}