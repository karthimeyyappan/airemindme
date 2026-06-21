package com.server.realsync.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.server.realsync.entity.Reminder;
import com.server.realsync.entity.ScheduleEntryStatus;

public interface ReminderRepository extends JpaRepository<Reminder, Integer> {

        long countByAccountIdAndReminderDate(
                        Integer accountId, 
                        LocalDate date);

        long countByAccountIdAndReminderPurposeIn(
                        Integer accountId, 
                        List<String> purposes);

        List<Reminder> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

        List<Reminder> findByAccountIdOrderByCreatedAtDesc(Integer accountId, org.springframework.data.domain.Pageable pageable);

        List<Reminder> findByCustomerIdAndAccountId(Integer customerId, Integer accountId);

        Optional<Reminder> findByIdAndAccountId(Integer id, Integer accountId);

        long countByAccountId(Integer accountId);

        long countByAccountIdAndStatus(Integer accountId, String status);

        long deleteByIdAndAccountId(Integer id, Integer accountId);

        // Filter by the Promotion Item (Plan or Product)
        List<Reminder> findByAccountIdAndAttachedItemTypeAndAttachedItemId(
                        Integer accountId, String type, Integer itemId);

        // Native query for Today's Stats
        @Query(value = "SELECT COUNT(*) FROM reminders " +
                        "WHERE account_id = :accountId " +
                        "AND status = 'Sent' " +
                        "AND DATE(created_at) = CURDATE()", nativeQuery = true)
        long countSentToday(@Param("accountId") Integer accountId);

        @Query(value = "SELECT * FROM reminders WHERE account_id = :accountId AND status = 'Scheduled' AND reminder_date >= CURDATE() ORDER BY reminder_date ASC LIMIT 3", nativeQuery = true)
        List<Reminder> findTop3UpcomingByAccountId(@Param("accountId") Integer accountId);

}