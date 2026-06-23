package com.server.realsync.services;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.realsync.entity.Account;
import com.server.realsync.repo.AccountRepository;

@Service
public class ReferralService {

    @Autowired
    private AccountRepository accountRepository;

    public Map<String, Object> getReferralInfo(Integer accountId) {

        // Find the logged-in account
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        // Auto-generate referral code if missing
        if (account.getReferralCode() == null) {
            account.setReferralCode("NUMEN-" + accountId);
            accountRepository.save(account);
        }

        // Find all accounts who were referred by this account
        List<Account> referredAccounts =
            accountRepository.findByReferredBy(accountId);

        // Build history list
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

        List<Map<String, String>> history = new ArrayList<>();
        for (Account a : referredAccounts) {
            Map<String, String> item = new HashMap<>();
            item.put("name", a.getName());
            item.put("joinedDate", a.getCreatedDate() != null
                ? a.getCreatedDate().format(formatter) : "-");
            history.add(item);
        }

        // Build final response
        Map<String, Object> response = new HashMap<>();
        response.put("referralCode", account.getReferralCode());
        response.put("history", history);

        return response;
    }
}