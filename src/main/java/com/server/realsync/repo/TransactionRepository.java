
package com.server.realsync.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Optional<Transaction> findByGatewayOrderId(String gatewayOrderId);

    List<Transaction> findByPaymentStatus(Transaction.PaymentStatus status);

    Page<Transaction> findByAccountId(Integer accountId, Pageable pageable);

    Page<Transaction> findByAccountIdAndCreatedAtBetween(
        Integer accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Transaction> findByAccountIdAndPaymentStatus(
        Integer accountId, Transaction.PaymentStatus status, Pageable pageable);

    Page<Transaction> findByAccountIdAndPaymentStatusAndCreatedAtBetween(
        Integer accountId, Transaction.PaymentStatus status,
        LocalDateTime from, LocalDateTime to, Pageable pageable);
}