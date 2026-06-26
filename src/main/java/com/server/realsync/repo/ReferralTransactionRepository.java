package com.server.realsync.repo;

import com.server.realsync.entity.ReferralTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.Transaction;

@Repository
public interface ReferralTransactionRepository extends JpaRepository<ReferralTransaction, Integer> {
    boolean existsByTransaction(Transaction transaction);
}
