package com.server.realsync.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 100, unique = true)
	private String email;

	@Column(nullable = false, length = 12, unique = true)
	private String mobile;

	@Column(length = 500)
	private String address;

	@Column(length = 100)
	private String upi;

	@Column(length = 50)
	private String category;

	@Column(length = 50)
	private String subcategory;

	@Column(name = "created_date")
	private LocalDateTime createdDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

	@Column(name = "business_name", length = 50)
	private String businessName;

	@Column(name = "gst_number", length = 20)
	private String gstNumber;

	@Column(name = "business_email", length = 100)
	private String businessEmail;

	@Column(name = "business_phone", length = 20)
	private String businessPhone;

	@Column(length = 50)
	private String country;

	@Column(length = 10)
	private String currency;

	@Column(length = 30)
	private String language;

	@Column(name = "timezone", length = 50)
	private String timezone;

	@Column(name = "date_format", length = 20)
	private String dateFormat;

	@Column(name = "number_format", length = 20)
	private String numberFormat;

	@Column(name = "customer_field_1_name", length = 100)
	private String customerField1Name;

	@Column(name = "customer_field_2_name", length = 100)
	private String customerField2Name;

	@Column(name = "customer_field_3_name", length = 100)
	private String customerField3Name;

	@Column(name = "customer_field_4_name", length = 100)
	private String customerField4Name;

	@Column(name = "customer_field_5_name", length = 100)
	private String customerField5Name;

	@Column(name = "referral_id", length = 50)
    private String referralId;

	@Column(name = "referred_by")
private Integer referredBy;

public Integer getReferredBy() {
    return referredBy;
}

public void setReferredBy(Integer referredBy) {
    this.referredBy = referredBy;
}

	// Getters and Setters
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

	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDateTime getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(LocalDateTime updatedDate) {
		this.updatedDate = updatedDate;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
	}

	@PrePersist
	protected void onCreate() {
		createdDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		updatedDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getUpi() {
		return upi;
	}

	public void setUpi(String upi) {
		this.upi = upi;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public void setSubcategory(String subcategory) {
		this.subcategory = subcategory;
	}

	public String getBusinessName() {
		return businessName;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	public String getGstNumber() {
		return gstNumber;
	}

	public void setGstNumber(String gstNumber) {
		this.gstNumber = gstNumber;
	}

	public String getBusinessEmail() {
		return businessEmail;
	}

	public void setBusinessEmail(String businessEmail) {
		this.businessEmail = businessEmail;
	}

	public String getBusinessPhone() {
		return businessPhone;
	}

	public void setBusinessPhone(String businessPhone) {
		this.businessPhone = businessPhone;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getCustomerField1Name() {
		return customerField1Name;
	}

	public void setCustomerField1Name(String customerField1Name) {
		this.customerField1Name = customerField1Name;
	}

	public String getCustomerField2Name() {
		return customerField2Name;
	}

	public void setCustomerField2Name(String customerField2Name) {
		this.customerField2Name = customerField2Name;
	}

	public String getCustomerField3Name() {
		return customerField3Name;
	}

	public void setCustomerField3Name(String customerField3Name) {
		this.customerField3Name = customerField3Name;
	}

	public String getCustomerField4Name() {
		return customerField4Name;
	}

	public void setCustomerField4Name(String customerField4Name) {
		this.customerField4Name = customerField4Name;
	}

	public String getCustomerField5Name() {
		return customerField5Name;
	}

	public void setCustomerField5Name(String customerField5Name) {
		this.customerField5Name = customerField5Name;
	}

	public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

}
