/**
 * 
 */
package com.server.realsync.repo;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.realsync.entity.PromotionExecutionLog;

/**
 * 
 */

public interface PromotionExecutionLogRepository extends JpaRepository<PromotionExecutionLog, Long> {

    List<PromotionExecutionLog> findByPromotionEntryId(Long promotionEntryId);

    void deleteByPromotionEntryId(Long promotionEntryId);
}