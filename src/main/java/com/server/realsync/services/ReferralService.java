package com.server.realsync.services;

import com.server.realsync.entity.Account;
import com.server.realsync.repo.AccountRepository;
import java.util.Optional;

import com.server.realsync.entity.Referral;
import com.server.realsync.repo.ReferralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReferralService {

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Get existing referral code for account, or create a new one
     */
   public String getOrCreateReferralCode(Long accountId) {
    // Get referral_id directly from account table
    Optional<Account> accountOpt = accountRepository.findById(accountId.intValue());
    
    if (accountOpt.isPresent()) {
        Account account = accountOpt.get();
        
        // If referral_id already set, return it
        if (account.getReferralId() != null && !account.getReferralId().isEmpty()) {
            return account.getReferralId();
        }
        
        // Generate new referral_id = NUMEN-{accountId}
        String newCode = "NUMEN-" + accountId;
        account.setReferralId(newCode);
        accountRepository.save(account);
        return newCode;
    }
    
    return "NUMEN-" + accountId;
}

    /**
     * Get list of people who signed up using this account's referral code
     */
    public List<Referral> getReferralHistory(Long accountId) {
        return referralRepository.findByAccountId(accountId)
            .stream()
            .filter(r -> r.getReferredAccountId() != null)
            .toList();
    }

    public long getTotalReferrals(Long accountId) {
        return referralRepository.countReferrals(accountId);
    }

    public long getCreditedReferrals(Long accountId) {
        return referralRepository.countCredited(accountId);
    }

    public double getTotalCredits(Long accountId) {
        return referralRepository.sumCredits(accountId);
    }
}