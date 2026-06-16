
package com.server.realsync.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.realsync.entity.ScheduleEntry;
import com.server.realsync.repo.ScheduleEntryRepository;

/**
 * 
 */

@Service
public class ScheduleEntryService {

    private final ScheduleEntryRepository scheduleEntryRepository;

    public ScheduleEntryService(ScheduleEntryRepository scheduleEntryRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    public ScheduleEntry save(ScheduleEntry entry) {
        return scheduleEntryRepository.save(entry);
    }

    public Optional<ScheduleEntry> getById(Long id) {
        return scheduleEntryRepository.findById(id);
    }

    public List<ScheduleEntry> getBySchedule(Long scheduleId) {
        return scheduleEntryRepository.findByScheduleId(scheduleId);
    }

    public List<ScheduleEntry> getDueEntries() {
        return scheduleEntryRepository.findByOccurrenceDateBefore(LocalDateTime.now());
    }

    public List<ScheduleEntry> getDueEntriesInWindow(LocalDateTime maxTime) {
        return scheduleEntryRepository.findByStatusAndOccurrenceDateBefore(com.server.realsync.entity.ScheduleEntryStatus.PENDING, maxTime);
    }

    public void delete(Long id) {
        scheduleEntryRepository.deleteById(id);
    }

}