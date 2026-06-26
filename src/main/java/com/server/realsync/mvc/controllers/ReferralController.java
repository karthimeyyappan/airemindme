package com.server.realsync.mvc.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.realsync.entity.Account;
import com.server.realsync.services.ReferralService;
import com.server.realsync.util.SecurityUtil;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    @GetMapping("/info")
    public ResponseEntity<?> getReferralInfo() {

        // Get logged-in account using SecurityUtil
        Account loggedIn = SecurityUtil.getCurrentAccountId();

        if (loggedIn == null || loggedIn.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Not logged in"));
        }

        Map<String, Object> data =
            referralService.getReferralInfo(loggedIn.getId());

        return ResponseEntity.ok(data);
    }
}