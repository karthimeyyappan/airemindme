package com.server.realsync.repo;

import com.server.realsync.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    // Get all referrals for an account
    List<Referral> findByAccountId(Long accountId);

    // Find by referral code
    Optional<Referral> findByReferralCode(String referralCode);

    // Count people who used this account's referral
    @Query("SELECT COUNT(r) FROM Referral r WHERE r.accountId = :id AND r.referredAccountId IS NOT NULL")
    long countReferrals(@Param("id") Long accountId);

    // Count credited referrals
    @Query("SELECT COUNT(r) FROM Referral r WHERE r.accountId = :id AND r.status = 'CREDITED'")
    long countCredited(@Param("id") Long accountId);

    // Sum total credits earned
    @Query("SELECT COALESCE(SUM(r.creditGiven), 0) FROM Referral r WHERE r.accountId = :id")
    double sumCredits(@Param("id") Long accountId);
}