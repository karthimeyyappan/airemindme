package com.server.realsync.repo;

import com.server.realsync.entity.ReferralTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface ReferralTransactionRepository extends JpaRepository<ReferralTransaction, Integer> {

    Page<ReferralTransaction> findByReferrerAccountId(Integer accountId, Pageable pageable);

    Page<ReferralTransaction> findByReferrerAccountIdAndCreatedDateBetween(
        Integer accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ReferralTransaction> findByReferrerAccountIdAndStatus(
        Integer accountId, ReferralTransaction.ReferralStatus status, Pageable pageable);

    Page<ReferralTransaction> findByReferrerAccountIdAndStatusAndCreatedDateBetween(
        Integer accountId, ReferralTransaction.ReferralStatus status,
        LocalDateTime from, LocalDateTime to, Pageable pageable);
}
