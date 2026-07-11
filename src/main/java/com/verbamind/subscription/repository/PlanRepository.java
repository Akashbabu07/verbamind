package com.verbamind.subscription.repository;

import com.verbamind.subscription.entity.Plan;
import com.verbamind.subscription.entity.PlanCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByCode(PlanCode code);
}