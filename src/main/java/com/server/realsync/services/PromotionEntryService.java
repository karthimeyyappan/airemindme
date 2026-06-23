/**
 * 
 */
package com.server.realsync.services;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.realsync.entity.PromotionEntry;
import com.server.realsync.repo.PromotionEntryRepository;

/**
 * 
 */

@Service
public class PromotionEntryService {

    private final PromotionEntryRepository promotionEntryRepository;

    public PromotionEntryService(PromotionEntryRepository promotionEntryRepository) {
        this.promotionEntryRepository = promotionEntryRepository;
    }

    public PromotionEntry save(PromotionEntry entry) {
        return promotionEntryRepository.save(entry);
    }

    public Optional<PromotionEntry> getById(Long id) {
        return promotionEntryRepository.findById(id);
    }

    public List<PromotionEntry> getByPromotion(Long promotionId) {
        return promotionEntryRepository.findByPromotionId(promotionId);
    }

    public List<PromotionEntry> getByCustomer(Integer customerId) {
        return promotionEntryRepository.findByCustomerId(customerId);
    }

    public void delete(Long id) {
        promotionEntryRepository.deleteById(id);
    }

    public void deleteByPromotionId(Long promotionId) {
        promotionEntryRepository.deleteByPromotionId(promotionId);
    }
}