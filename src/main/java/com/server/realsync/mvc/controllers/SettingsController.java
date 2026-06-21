package com.server.realsync.mvc.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.dto.PasswordResetDto;
import com.server.realsync.dto.SettingsUpdateDto;
import com.server.realsync.dto.CustomerFieldSettingsRequest;
import com.server.realsync.dto.CustomerFieldSettingsResponse;
import com.server.realsync.entity.Account;
import com.server.realsync.entity.User;
import com.server.realsync.services.AccountService;
import com.server.realsync.services.UserService;
import com.server.realsync.services.SettingsService;
import com.server.realsync.util.SecurityUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping
public class SettingsController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================
    // UPDATE PROFILE
    // ==========================
    @PostMapping("/api/settings/profile")
    public String updateProfile(@RequestBody Account req) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        Account account = accountService.getById(accountId);

        account.setName(req.getName());
        account.setEmail(req.getEmail());
        account.setMobile(req.getMobile());

        accountService.save(account);
        return "Profile Updated";
    }

    @PutMapping("/api/settings/account/business-details")
    public ResponseEntity<String> updateBusinessDetails(@RequestBody Account req) {
        Integer accountId = SecurityUtil.getCurrentAccountId().getId();
        Account account = accountService.getById(accountId);

        if (req.getBusinessName() != null)
            account.setBusinessName(req.getBusinessName());

        if (req.getBusinessEmail() != null)
            account.setBusinessEmail(req.getBusinessEmail());

        if (req.getBusinessPhone() != null)
            account.setBusinessPhone(req.getBusinessPhone());

        if (req.getGstNumber() != null)
            account.setGstNumber(req.getGstNumber());

        if (req.getAddress() != null)
            account.setAddress(req.getAddress());

        if (req.getCategory() != null)
            account.setCategory(req.getCategory());

        if (req.getSubcategory() != null)
            account.setSubcategory(req.getSubcategory());

        accountService.save(account);
        return ResponseEntity.ok("Updated");
    }

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    // ==========================
    // UPDATE PASSWORD
    // ==========================
    @PostMapping("/api/settings/password")
    public String updatePassword(@RequestBody PasswordResetDto dto) {
        User user = SecurityUtil.getLoggedInUser();
        if (user == null) {
            return "Unauthorized";
        }

        // Verify current password before changing
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            return "Current password is incorrect";
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.saveUser(user);

        return "Password Updated Successfully";
    }

    @GetMapping("/api/settings/localization")
    public ResponseEntity<?> getLocalizationSettings() {

        Account loggedIn = SecurityUtil.getCurrentAccountId();

        if (loggedIn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Session expired"));
        }

        Account account = accountService.getById(loggedIn.getId());

        return ResponseEntity.ok(Map.of(
                "country", account.getCountry() != null ? account.getCountry() : "India",
                "timezone", account.getTimezone() != null ? account.getTimezone() : "Asia/Kolkata",
                "language", account.getLanguage() != null ? account.getLanguage() : "en",
                "currency", account.getCurrency() != null ? account.getCurrency() : "INR"));
    }

    // ==========================================
    // UNIFIED SETTINGS UPDATE (PROFILE + BUSINESS + REGION)
    // ==========================================
    @PostMapping("/api/account/settings/update")
    public ResponseEntity<?> updateSettings(@RequestBody SettingsUpdateDto req) {
        Account loggedIn = SecurityUtil.getCurrentAccountId();
        if (loggedIn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "User session not found."));
        }

        Account account = accountService.getById(loggedIn.getId());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Account not found."));
        }

        // Profile Section
        if (req.getName() != null) {
            account.setName(req.getName());
        }
        if (req.getEmail() != null) {
            account.setEmail(req.getEmail());
        }
        if (req.getMobile() != null) {
            account.setMobile(req.getMobile());
        }

        // Business Details Section
        if (req.getBusinessName() != null) {
            account.setBusinessName(req.getBusinessName());
        }
        if (req.getGstNumber() != null) {
            account.setGstNumber(req.getGstNumber());
        }
        if (req.getBusinessEmail() != null) {
            account.setBusinessEmail(req.getBusinessEmail());
        }
        if (req.getBusinessPhone() != null) {
            account.setBusinessPhone(req.getBusinessPhone());
        }
        if (req.getCategory() != null) {
            account.setCategory(req.getCategory());
        }
        if (req.getSubcategory() != null) {
            account.setSubcategory(req.getSubcategory());
        }
        if (req.getAddress() != null) {
            account.setAddress(req.getAddress());
        }

        // Region & Localization Section
        if (req.getCountry() != null) {
            account.setCountry(req.getCountry());
        }
        if (req.getTimezone() != null) {
            account.setTimezone(req.getTimezone());
        }
        if (req.getLanguage() != null) {
            account.setLanguage(req.getLanguage());
        }
        if (req.getCurrency() != null) {
            account.setCurrency(req.getCurrency());
        }

        accountService.save(account);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Settings updated successfully"));
    }

    @GetMapping("/api/settings/customer-fields")
    public ResponseEntity<CustomerFieldSettingsResponse> getCustomerFields() {
        CustomerFieldSettingsResponse response = settingsService.getCustomerFieldSettings();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/settings/customer-fields")
    public ResponseEntity<CustomerFieldSettingsResponse> updateCustomerFields(
            @RequestBody CustomerFieldSettingsRequest request) {
        CustomerFieldSettingsResponse response = settingsService.updateCustomerFieldSettings(request);
        return ResponseEntity.ok(response);
    }
}