package com.server.realsync.mvc.controllers;

import com.server.realsync.model.Referral;
import com.server.realsync.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    /**
     * GET /api/referrals/info
     * Called by settings page Referrals tab to load all referral data
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getReferralInfo(HttpSession session) {

        // ⚠️ IMPORTANT: Check how your app stores logged-in account ID in session
        // Look at your other controllers for: session.getAttribute("???")
        // Common keys used: "accountId", "account", "userId", "loggedInAccount"
        // Replace "accountId" below with the correct key from your project

        Long accountId = (Long) session.getAttribute("accountId");

        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

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