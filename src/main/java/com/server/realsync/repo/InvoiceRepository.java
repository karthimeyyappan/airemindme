package com.server.realsync.repo;

import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoiceStatus;
import com.server.realsync.dto.TopCustomerDTO;
import com.server.realsync.dto.RecentInvoiceDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("""
        SELECT 
            COALESCE(SUM(i.grandTotal), 0),
            COALESCE(SUM(i.balanceAmount), 0),
            COUNT(i),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.PAID THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.SENT THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.PARTIALLY_PAID THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.OVERDUE THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.DRAFT THEN 1 ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN i.status = com.server.realsync.entity.InvoiceStatus.OVERDUE THEN i.balanceAmount ELSE 0 END), 0),
            COUNT(DISTINCT i.customerId)
        FROM Invoice i 
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status != com.server.realsync.entity.InvoiceStatus.CANCELLED
    """)
    Object getInvoiceDashboardMetrics(@Param("accountId") Integer accountId);

    @Query("""
        SELECT i FROM Invoice i 
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.invoiceDate >= :startDate
        AND i.status != com.server.realsync.entity.InvoiceStatus.CANCELLED
    """)
    List<Invoice> findInvoicesForRevenueAnalytics(@Param("accountId") Integer accountId, @Param("startDate") LocalDate startDate);

    @Query("""
        SELECT new com.server.realsync.dto.TopCustomerDTO(i.customerName, SUM(i.grandTotal))
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status != com.server.realsync.entity.InvoiceStatus.CANCELLED
        GROUP BY i.customerName
        ORDER BY SUM(i.grandTotal) DESC
    """)
    List<TopCustomerDTO> findTop5Customers(@Param("accountId") Integer accountId, Pageable pageable);

    @Query("""
        SELECT new com.server.realsync.dto.RecentInvoiceDTO(
            i.id, i.invoiceNumber, i.customerName, i.invoiceDate, i.grandTotal, i.paidAmount, i.balanceAmount, i.status
        )
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        ORDER BY i.id DESC
    """)
    List<RecentInvoiceDTO> findRecentInvoices(@Param("accountId") Integer accountId, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(i.balanceAmount), 0)
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status NOT IN (com.server.realsync.entity.InvoiceStatus.PAID, com.server.realsync.entity.InvoiceStatus.CANCELLED, com.server.realsync.entity.InvoiceStatus.DRAFT)
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
    """)
    java.math.BigDecimal getOutstandingBalance(@Param("accountId") Integer accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COALESCE(SUM(i.balanceAmount), 0)
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status NOT IN (com.server.realsync.entity.InvoiceStatus.PAID, com.server.realsync.entity.InvoiceStatus.CANCELLED, com.server.realsync.entity.InvoiceStatus.DRAFT)
        AND i.dueDate < :today
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
    """)
    java.math.BigDecimal getOverdueBalance(@Param("accountId") Integer accountId, @Param("today") LocalDate today, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COUNT(i)
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status NOT IN (com.server.realsync.entity.InvoiceStatus.PAID, com.server.realsync.entity.InvoiceStatus.CANCELLED, com.server.realsync.entity.InvoiceStatus.DRAFT)
        AND i.dueDate < :today
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
    """)
    long getOverdueCount(@Param("accountId") Integer accountId, @Param("today") LocalDate today, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COALESCE(SUM(i.balanceAmount), 0)
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status NOT IN (com.server.realsync.entity.InvoiceStatus.PAID, com.server.realsync.entity.InvoiceStatus.CANCELLED, com.server.realsync.entity.InvoiceStatus.DRAFT)
        AND i.dueDate = :today
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
    """)
    java.math.BigDecimal getDueTodayBalance(@Param("accountId") Integer accountId, @Param("today") LocalDate today, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COUNT(i)
        FROM Invoice i
        WHERE i.customerId IN (SELECT CAST(c.id AS long) FROM Customer c WHERE c.accountId = :accountId)
        AND i.status NOT IN (com.server.realsync.entity.InvoiceStatus.PAID, com.server.realsync.entity.InvoiceStatus.CANCELLED, com.server.realsync.entity.InvoiceStatus.DRAFT)
        AND i.dueDate = :today
        AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
        AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
    """)
    long getDueTodayCount(@Param("accountId") Integer accountId, @Param("today") LocalDate today, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Invoice> findByCustomerId(Long customerId);

    @Query(value = "SELECT invoice_number FROM invoices ORDER BY id DESC LIMIT 1", nativeQuery = true)
    String findLatestInvoiceNumber();
}