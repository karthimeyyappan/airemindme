package com.server.realsync.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.server.realsync.entity.CreditTransaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Integer> {
    List<CreditTransaction> findByAccountId(Integer accountId);

    Page<CreditTransaction> findByAccountId(Integer accountId, Pageable pageable);

    Page<CreditTransaction> findByAccountIdAndCreatedDateBetween(
            Integer accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<CreditTransaction> findByAccountIdAndTypeContainingIgnoreCaseAndCreatedDateBetween(
            Integer accountId, String type, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
