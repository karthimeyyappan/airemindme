package com.server.realsync.dto;

import java.util.List;

public class TemplateGenerateResponse {
    private String content;
    private String title;
    private String description;
    private Integer variantCount;
    private List<String> variants;

    public TemplateGenerateResponse() {}

    public TemplateGenerateResponse(String content) {
        this.content = content;
    }

    public TemplateGenerateResponse(String title, String description, Integer variantCount, List<String> variants) {
        this.title = title;
        this.description = description;
        this.variantCount = variantCount;
        this.variants = variants;
        if (variants != null && !variants.isEmpty()) {
            this.content = variants.get(0);
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(Integer variantCount) {
        this.variantCount = variantCount;
    }

    public List<String> getVariants() {
        return variants;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }
}
