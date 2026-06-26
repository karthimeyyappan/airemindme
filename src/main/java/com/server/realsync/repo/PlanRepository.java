package com.server.realsync.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.Plan;
import java.util.Optional;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    Optional<Plan> findByNameIgnoreCase(String name);
    Optional<Plan> findByIsTrial(Boolean isTrial);
    List<Plan> findByIsActiveTrueOrderByPriceInrAsc();
    List<Plan> findByIsActiveTrueAndIsTrialFalseOrderByPriceInrAsc();
}