package com.server.realsync.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.AccountPlan;

@Repository
public interface AccountPlanRepository extends JpaRepository<AccountPlan, Integer> {
	Optional<AccountPlan> findByAccountId(Integer accountId);

	Optional<AccountPlan> findByAccountIdAndStatus(Integer accountId, AccountPlan.PlanStatus status);

	
}