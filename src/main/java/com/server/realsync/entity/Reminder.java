package com.server.realsync.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(length = 255)
    private String title;

    @Column(length = 1000)
    private String message;
    @Column(name = "reminder_type")
    private String reminderType;

    @Column(name = "recurring", nullable = false)
    private Boolean recurring = false;

    @Column(length = 255)
    private String frequency;

    @Column(name = "total_occurrences")
    private Integer totalOccurrences;

    private Double amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "channel", length = 255)
    private String channel;

    @Column(length = 255)
    private String status = "Scheduled";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "attached_item_id")
    private Integer attachedItemId;

    @Column(name = "attached_item_type", length = 20)
    private String attachedItemType; // "plan" or "product"

    @Column(name = "reminder_purpose", length = 50)
    private String reminderPurpose; // "payment", "service", "followup", "general"

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===================== Getters & Setters =====================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public Boolean getRecurring() {
        return recurring;
    }

    public void setRecurring(Boolean recurring) {
        this.recurring = recurring;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Integer getTotalOccurrences() {
        return totalOccurrences;
    }

    public void setTotalOccurrences(Integer totalOccurrences) {
        this.totalOccurrences = totalOccurrences;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttachedItemId() {
        return attachedItemId;
    }

    public void setAttachedItemId(Integer attachedItemId) {
        this.attachedItemId = attachedItemId;
    }

    public String getAttachedItemType() {
        return attachedItemType;
    }

    public void setAttachedItemType(String attachedItemType) {
        this.attachedItemType = attachedItemType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReminderPurpose() {
        return reminderPurpose;
    }

    public void setReminderPurpose(String reminderPurpose) {
        this.reminderPurpose = reminderPurpose;
    }
}