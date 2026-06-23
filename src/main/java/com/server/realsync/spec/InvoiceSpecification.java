package com.server.realsync.spec;

import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoiceStatus;
import com.server.realsync.entity.Customer;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;

public class InvoiceSpecification {

    public static Specification<Invoice> filter(String search, Long customerId, String status, LocalDate dateFrom, LocalDate dateTo, Integer accountId) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toUpperCase() + "%";
                Predicate searchInvoice = cb.like(cb.upper(root.get("invoiceNumber")), like);
                Predicate searchCustomer = cb.like(cb.upper(root.get("customerName")), like);
                p = cb.and(p, cb.or(searchInvoice, searchCustomer));
            }

            if (customerId != null) {
                p = cb.and(p, cb.equal(root.get("customerId"), customerId));
            }

            if (dateFrom != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("invoiceDate"), dateFrom));
            }

            if (dateTo != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("invoiceDate"), dateTo));
            }

            if (accountId != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Customer> cust = sub.from(Customer.class);
                sub.select(cust.get("id").as(Long.class));
                sub.where(cb.equal(cust.get("accountId"), accountId));
                p = cb.and(p, root.get("customerId").in(sub));
            }

            if (status != null && !status.isBlank()) {
                LocalDate today = LocalDate.now();
                String upperStatus = status.trim().toUpperCase();
                if ("DUE_TODAY".equals(upperStatus)) {
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.PAID));
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED));
                    p = cb.and(p, cb.equal(root.get("dueDate"), today));
                } else if ("OVERDUE".equals(upperStatus)) {
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.PAID));
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED));
                    p = cb.and(p, cb.lessThan(root.get("dueDate"), today));
                } else if ("UPCOMING".equals(upperStatus)) {
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.PAID));
                    p = cb.and(p, cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED));
                    p = cb.and(p, cb.greaterThan(root.get("dueDate"), today));
                } else if ("PAID".equals(upperStatus)) {
                    p = cb.and(p, cb.equal(root.get("status"), InvoiceStatus.PAID));
                } else {
                    try {
                        InvoiceStatus enumStatus = InvoiceStatus.valueOf(upperStatus);
                        p = cb.and(p, cb.equal(root.get("status"), enumStatus));
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid status enum mappings
                    }
                }
            }

            return p;
        };
    }
}
