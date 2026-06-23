package com.server.realsync.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.server.realsync.entity.InvoicePayment;
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Integer> {

    List<InvoicePayment> findByInvoiceIdOrderByPaymentDateDesc(Integer invoiceId);

    List<InvoicePayment> findByAccountId(Integer accountId);

    List<InvoicePayment> findByAccountIdOrderByPaymentDateDesc(
            Integer accountId);

    List<InvoicePayment> findByAccountIdAndInvoiceId(
            Integer accountId,
            Integer invoiceId);

    @Query("""
            SELECT COALESCE(SUM(p.amount),0)
            FROM InvoicePayment p
            WHERE p.invoiceId = :invoiceId
            """)
    Double getTotalPaid(Integer invoiceId);

    @Query("""
            SELECT COALESCE(SUM(p.amount),0.0)
            FROM InvoicePayment p
            WHERE p.accountId = :accountId
            """)
    Double sumPaymentsByAccountId(@Param("accountId") Integer accountId);

    List<InvoicePayment> findByInvoiceIdInOrderByPaymentDateDesc(List<Integer> invoiceIds);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0.0)
        FROM InvoicePayment p
        WHERE p.accountId = :accountId
        AND (:startDate IS NULL OR p.paymentDate >= :startDate)
        AND (:endDate IS NULL OR p.paymentDate <= :endDate)
    """)
    Double getCollectedAmount(@Param("accountId") Integer accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}