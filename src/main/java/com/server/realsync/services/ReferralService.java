package com.server.realsync.service;

import com.server.realsync.model.Referral;
import com.server.realsync.repository.ReferralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReferralService {

    @Autowired
    private ReferralRepository referralRepository;

    /**
     * Get existing referral code for account, or create a new one
     */
    public String getOrCreateReferralCode(Long accountId) {
        List<Referral> list = referralRepository.findByAccountId(accountId);

        // Find master code row (no referred account linked yet = the owner's code row)
        Optional<Referral> master = list.stream()
            .filter(r -> r.getReferredAccountId() == null)
            .findFirst();

        if (master.isPresent()) {
            return master.get().getReferralCode();
        }

        // Create a brand new referral code for this account
        Referral r = new Referral();
        r.setAccountId(accountId);
        r.setReferralCode("NUMEN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        referralRepository.save(r);
        return r.getReferralCode();
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