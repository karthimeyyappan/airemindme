
package com.server.realsync.repo;

import java.util.List;

/**
 * 
 */

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.realsync.entity.PromotionEntry;

public interface PromotionEntryRepository extends JpaRepository<PromotionEntry, Long> {

    List<PromotionEntry> findByPromotionId(Long promotionId);

    List<PromotionEntry> findByCustomerId(Integer customerId);

    List<PromotionEntry> findByPromotionIdAndSentWhatsappFalse(Long promotionId);

    void deleteByPromotionId(Long promotionId);
}