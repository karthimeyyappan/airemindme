/**
 * 
 */
package com.server.realsync.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.server.realsync.entity.Promotion;
import com.server.realsync.repo.PromotionRepository;

/**
 * 
 */

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Promotion save(Promotion promotion) {
        return promotionRepository.save(promotion);
    }

    public List<Promotion> getByAccount(Integer accountId) {
        return promotionRepository.findByAccountId(accountId);
    }

    public Optional<Promotion> getById(Long id) {
        return promotionRepository.findById(id);
    }

    public List<Promotion> getScheduledPromotions(Integer accountId) {
        return promotionRepository.findByAccountIdAndScheduledAtIsNotNull(accountId);
    }

    public long getTotalPromotions(Integer accountId) {
        return promotionRepository.countByAccountId(accountId);
    }

    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    public void deleteById(Long id) {
        promotionRepository.deleteById(id);
    }
}