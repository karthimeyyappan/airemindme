package com.server.realsync.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.Account;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
	Optional<Account> findByEmail(String email);
	Optional<Account> findByMobile(String mobile);

	 // Referral system
    List<Account> findByReferredBy(Integer referredBy);
    Optional<Account> findByReferralCode(String referralCode);
}
