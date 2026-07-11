package com.verbamind.usage.repository;

import com.verbamind.usage.entity.UsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageDailyRepository extends JpaRepository<UsageDaily, UUID> {

    Optional<UsageDaily> findByOrganizationIdAndUsageDate(UUID organizationId, LocalDate usageDate);

    @Query("""
           SELECT u FROM UsageDaily u
           WHERE u.organizationId = :organizationId
             AND u.usageDate BETWEEN :from AND :to
           """)
    List<UsageDaily> findInRange(@Param("organizationId") UUID organizationId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}