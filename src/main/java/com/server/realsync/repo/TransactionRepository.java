
package com.server.realsync.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Optional<Transaction> findByGatewayOrderId(String gatewayOrderId);

    List<Transaction> findByPaymentStatus(Transaction.PaymentStatus status);
}