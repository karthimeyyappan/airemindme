package com.server.realsync.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.entity.ScheduleEntryStatus;

/**
 * 
 */

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    @Query("SELECT COUNT(se) FROM ScheduleEntry se JOIN Schedule s ON se.scheduleId = s.id WHERE s.accountId = :accountId")
    long countByAccountId(@Param("accountId") Integer accountId);

    @Query("SELECT se.occurrenceDate, s.sourceType FROM ScheduleEntry se JOIN Schedule s ON se.scheduleId = s.id WHERE s.accountId = :accountId AND se.occurrenceDate >= :startDateTime AND se.occurrenceDate <= :endDateTime")
    List<Object[]> findScheduleEntriesForActivityChart(
        @Param("accountId") Integer accountId,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    List<ScheduleEntry> findByScheduleId(Long scheduleId);

    List<ScheduleEntry> findByOccurrenceDateBefore(LocalDateTime time);

    List<ScheduleEntry> findByStatusAndOccurrenceDateBefore(ScheduleEntryStatus status, LocalDateTime time);

    List<ScheduleEntry> findByStatus(ScheduleEntryStatus status);

    List<ScheduleEntry> findByReminderIdOrderByOccurrenceDateAsc(Long reminderId);

    List<ScheduleEntry> findTop2ByReminderIdOrderByOccurrenceDateDesc(Long reminderId);

    List<ScheduleEntry> findBySourceTypeAndSourceId(String sourceType,Long sourceId);

    boolean existsByReminderIdAndStatus(Long reminderId,ScheduleEntryStatus status);

    @Modifying
    void deleteByReminderIdAndStatusNot(Long reminderId, ScheduleEntryStatus status);

    @Modifying
    @Transactional
    void deleteBySourceIdAndSourceTypeAndStatusNot(Long sourceId, String sourceType, ScheduleEntryStatus status);

    @Modifying
    @Transactional
    void deleteBySourceIdAndSourceType(Long sourceId, String sourceType);

    List<ScheduleEntry> findByAccountIdAndOccurrenceDateBetween(
        Integer accountId, 
        LocalDateTime start, 
        LocalDateTime end);

    List<ScheduleEntry> findByAccountIdAndOccurrenceDateBeforeAndStatusNot(
        Integer accountId, 
        LocalDateTime date, 
        ScheduleEntryStatus status);

    long countByAccountIdAndStatus(
        Integer accountId, 
        ScheduleEntryStatus status);

}