package com.server.realsync.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.server.realsync.entity.Report;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    List<Report> findByAccountId(Integer accountId);

    List<Report> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Report findByReportNumber(String reportNumber);
}
