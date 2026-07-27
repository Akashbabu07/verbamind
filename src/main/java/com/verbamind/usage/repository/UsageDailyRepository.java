package com.verbamind.usage.repository;

import com.verbamind.usage.entity.UsageDaily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsageDailyRepository extends JpaRepository<UsageDaily, UUID> {

    Optional<UsageDaily> findByOrganizationIdAndUsageDate(UUID organizationId, LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UsageDaily u WHERE u.organizationId = :organizationId AND u.usageDate = :usageDate")
    Optional<UsageDaily> findByOrganizationIdAndUsageDateForUpdate(@Param("organizationId") UUID organizationId,
                                                                    @Param("usageDate") LocalDate usageDate);

    @Query("""
           SELECT u FROM UsageDaily u
           WHERE u.organizationId = :organizationId
             AND u.usageDate BETWEEN :from AND :to
           """)
    List<UsageDaily> findInRange(@Param("organizationId") UUID organizationId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(u.aiRequests), 0) FROM UsageDaily u WHERE u.usageDate = :date")
    long sumAiRequestsForDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(u.aiRequests), 0) FROM UsageDaily u WHERE u.usageDate BETWEEN :from AND :to")
    long sumAiRequestsInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(u.tokensUsed), 0) FROM UsageDaily u WHERE u.usageDate BETWEEN :from AND :to")
    long sumTokensInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}