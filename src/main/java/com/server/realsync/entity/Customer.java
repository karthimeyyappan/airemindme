package com.server.realsync.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "customer", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_account_mobile", columnNames = { "account_id", "mobile" }),

})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String mobile;
    
    @Column(length = 50)
    private String gstNo;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country;

    @Column(name = "channel")
    private String channel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    @Column(name = "customer_group_id")
    private String customerGroupId; // Now supports "1,2,5"

    @Column(name = "customer_field_1", length = 255)
    private String customerField1;

    @Column(name = "customer_field_2", length = 255)
    private String customerField2;

    @Column(name = "customer_field_3", length = 255)
    private String customerField3;

    @Column(name = "customer_field_4", length = 255)
    private String customerField4;

    @Column(name = "customer_field_5", length = 255)
    private String customerField5;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Customer() {
    }

    // Getters & Setters

    public Integer getId() {
        return id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public LocalDate getWeddingDate() {
        return weddingDate;
    }

    public void setWeddingDate(LocalDate weddingDate) {
        this.weddingDate = weddingDate;
    }

    public String getCustomerGroupId() {
        return customerGroupId;
    }

    public void setCustomerGroupId(String customerGroupId) {
        this.customerGroupId = customerGroupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCustomerField1() {
        return customerField1;
    }

    public void setCustomerField1(String customerField1) {
        this.customerField1 = customerField1;
    }

    public String getCustomerField2() {
        return customerField2;
    }

    public void setCustomerField2(String customerField2) {
        this.customerField2 = customerField2;
    }

    public String getCustomerField3() {
        return customerField3;
    }

    public void setCustomerField3(String customerField3) {
        this.customerField3 = customerField3;
    }

    public String getCustomerField4() {
        return customerField4;
    }

    public void setCustomerField4(String customerField4) {
        this.customerField4 = customerField4;
    }

    public String getCustomerField5() {
        return customerField5;
    }

    public void setCustomerField5(String customerField5) {
        this.customerField5 = customerField5;
    }
}