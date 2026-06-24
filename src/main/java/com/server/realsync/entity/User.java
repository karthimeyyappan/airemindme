package com.server.realsync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;// mobile number

	@Column(nullable = false)
	private String password;

	@Column(nullable = true)
	private String otp;

	@Column(nullable = true)
	private String email;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false)
	private String mobile;

	@ManyToOne
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Transient
	private String code;

	@Column(nullable = true)
	private String resetToken;

	@Column(nullable = true)
	private java.time.LocalDateTime resetTokenExpiry;

	// Getter & Setter
	public String getResetToken() {
		return resetToken;
	}

	public void setResetToken(String resetToken) {
		this.resetToken = resetToken;
	}

	public java.time.LocalDateTime getResetTokenExpiry() {
		return resetTokenExpiry;
	}

	public void setResetTokenExpiry(java.time.LocalDateTime resetTokenExpiry) {
		this.resetTokenExpiry = resetTokenExpiry;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	// Dynamic Role-based Code
	public String getCode() {
		if (id == null || role == null || role.getName() == null) {
			return "N/A";
		}

		String prefix;
		switch (role.getName().toUpperCase()) {
			case "ADMIN":
				prefix = "AD";
				break;
			case "LEAD PROVIDER":
				prefix = "LP";
				break;
			case "MANAGER":
				prefix = "MG";
				break;
			case "CHANNEL PARTNER":
				prefix = "CP";
				break;
			case "BROKER":
				prefix = "BK";
				break;
			default:
				prefix = "US";
		}

		// Zero-pad ID (e.g., 1 -> 0001)
		return String.format("%s%04d", prefix, id);
	}

	public void setCode(String code) {
		this.code = code;
	}

}
