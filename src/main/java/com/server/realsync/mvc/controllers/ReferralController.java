package com.server.realsync.mvc.controllers;

import com.server.realsync.entity.Referral;
import com.server.realsync.services.ReferralService;
import com.server.realsync.entity.Account;
import com.server.realsync.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getReferralInfo() {

        Account loggedIn = SecurityUtil.getCurrentAccountId();

        if (loggedIn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Not logged in"));
        }

        Long accountId = Long.valueOf(loggedIn.getId());

        String code = referralService.getOrCreateReferralCode(accountId);
        long total = referralService.getTotalReferrals(accountId);
        long credited = referralService.getCreditedReferrals(accountId);
        double credits = referralService.getTotalCredits(accountId);
        List<Referral> history = referralService.getReferralHistory(accountId);

        Map<String, Object> response = new HashMap<>();
        response.put("referralCode", code);
        response.put("totalReferrals", total);
        response.put("creditedReferrals", credited);
        response.put("totalCredits", credits);
        response.put("history", history);

        return ResponseEntity.ok(response);
    }
}