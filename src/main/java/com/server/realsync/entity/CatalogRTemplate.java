package com.server.realsync.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Entity
@Table(name = "catalog_rtemplate")
public class CatalogRTemplate {

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

    /**
     * Stored as comma-separated string:
     * Example: "Hemoglobin,WBC,Platelets"
     */
    @Column(columnDefinition = "TEXT")
    private String columns;

    /**
     * Price per report
     */
    private Double price;

    /**
     * Show grand total row
     */
    @Column(name = "show_total")
    private Boolean showTotal = true;

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDate.now();
        if (status == null)
            status = "active";
        if (showTotal == null)
            showTotal = true;
    }

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
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getShowTotal() {
        return showTotal;
    }

    public void setShowTotal(Boolean showTotal) {
        this.showTotal = showTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate d) {
        this.createdAt = d;
    }

    public List<Map<String, String>> getParsedColumns() {
        if (this.columns == null || this.columns.trim().isEmpty()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println("RAW COLUMNS = " + columns);

            List<Map<String, String>> rawList = mapper.readValue(
                    this.columns,
                    new TypeReference<List<Map<String, String>>>() {
                    });

            return rawList.stream().map(rawMap -> {
                Map<String, String> normalized = new java.util.HashMap<>();
                
                // fieldName fallback logic
                String fieldName = rawMap.get("fieldName");
                if (fieldName == null) {
                    fieldName = rawMap.get("test");
                }
                if (fieldName == null) {
                    fieldName = rawMap.get("name");
                }
                normalized.put("fieldName", fieldName != null ? fieldName : "");

                // fieldType fallback logic
                String fieldType = rawMap.get("fieldType");
                normalized.put("fieldType", fieldType != null ? fieldType : "Text");

                // description / hint fallback logic
                String description = rawMap.get("description");
                if (description == null) {
                    description = rawMap.get("hint");
                }
                if (description == null) {
                    description = rawMap.get("range");
                }
                normalized.put("description", description != null ? description : "");
                normalized.put("hint", description != null ? description : "");

                return normalized;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            try {
                System.out.println("Fallback parsing comma-separated columns: " + this.columns);
                return Arrays.stream(this.columns.split(","))
                        .map(String::trim)
                        .filter(c -> !c.isEmpty())
                        .map(c -> {
                            Map<String, String> m = new java.util.HashMap<>();
                            m.put("fieldName", c);
                            m.put("fieldType", "Text");
                            m.put("description", "");
                            m.put("hint", "");
                            return m;
                        })
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                ex.printStackTrace();
                return List.of();
            }
        }
    }


}