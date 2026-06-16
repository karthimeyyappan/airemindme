package com.server.realsync.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "catalog_template")
public class CatalogTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(nullable = false)
    private String title;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Stored as a comma-separated string, e.g. "WhatsApp,SMS,Email"
     * Use getChannelList() / setChannelList() for Java usage.
     */
    @Column(name = "channels")
    private String channels;

    @Column(length = 20)
    private String language;

    @Column(length = 50)
    private String purpose;

    @Column(name = "template_type", length = 50)
    private String templateType;

    @Column(name = "name")
    private String name;

    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDate createdDate;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "variant_count")
    private Integer variantCount = 1;

    @Column(name = "variants_json", columnDefinition = "LONGTEXT")
    private String variantsJson;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDate.now();
        if (createdDate == null)
            createdDate = LocalDate.now();
        if (status == null)
            status = "active";
        syncFields();
    }

    @PreUpdate
    protected void onUpdate() {
        syncFields();
    }

    private void syncFields() {
        if (name != null) {
            this.title = name;
        } else if (title != null) {
            this.name = title;
        }

        if (prompt != null) {
            this.description = prompt;
        } else if (description != null) {
            this.prompt = description;
        }

        if (createdDate != null) {
            this.createdAt = createdDate;
        } else if (createdAt != null) {
            this.createdDate = createdAt;
        }

        if (active != null) {
            this.status = active ? "active" : "inactive";
        } else if ("active".equals(status)) {
            this.active = true;
        } else if ("inactive".equals(status)) {
            this.active = false;
        }
    }

    // ── Helpers for channels list ──────────────────────────────────────

    @Transient
    public List<String> getChannelList() {
        if (channels == null || channels.isBlank())
            return List.of();
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Transient
    public void setChannelList(List<String> list) {
        this.channels = (list == null || list.isEmpty())
                ? ""
                : String.join(",", list);
    }

    // ── Getters & Setters ──────────────────────────────────────────────

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.name = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        this.description = desc;
        this.prompt = desc;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.title = name;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        this.description = prompt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        this.status = active ? "active" : "inactive";
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
        this.createdAt = createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.active = "active".equals(status);
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate d) {
        this.createdAt = d;
        this.createdDate = d;
    }

    public Integer getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(Integer variantCount) {
        this.variantCount = variantCount;
    }

    public String getVariantsJson() {
        return variantsJson;
    }

    public void setVariantsJson(String variantsJson) {
        this.variantsJson = variantsJson;
    }
}