package com.server.realsync.mvc.controllers;

import com.server.realsync.entity.Account;
import com.server.realsync.repo.AccountRepository;
import com.server.realsync.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getReferralInfo() {

        Account loggedIn = SecurityUtil.getCurrentAccountId();
        if (loggedIn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Not logged in"));
        }

        // Get full account details
        Account account = accountRepository.findById(loggedIn.getId()).orElse(null);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Account not found"));
        }

        // Auto-generate referral code if not set
        if (account.getReferralId() == null || account.getReferralId().isEmpty()) {
            account.setReferralId("NUMEN-" + account.getId());
            accountRepository.save(account);
        }

        // Find all accounts referred by this account
        List<Account> referredAccounts = accountRepository.findByReferredBy(account.getId());

        // Build history list
        List<Map<String, Object>> history = new ArrayList<>();
        for (Account ref : referredAccounts) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",      ref.getName());
            item.put("email",     ref.getEmail());
            item.put("joinedDate", ref.getCreatedDate() != null ?
                ref.getCreatedDate().toString().substring(0, 10) : "—");
            history.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("referralCode",   account.getReferralId());
        response.put("totalReferrals", referredAccounts.size());
        response.put("history",        history);

        return ResponseEntity.ok(response);
    }
}