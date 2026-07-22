package com.verbamind.usage.service;

import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.subscription.entity.Plan;
import com.verbamind.subscription.service.SubscriptionService;
import com.verbamind.usage.dto.UsageResponse;
import com.verbamind.usage.entity.UsageDaily;
import com.verbamind.usage.exception.QuotaExceededException;
import com.verbamind.usage.exception.StorageQuotaExceededException;
import com.verbamind.usage.repository.UsageDailyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class UsageService {

    private final UsageDailyRepository usageDailyRepository;
    private final DocumentRepository documentRepository;
    private final SubscriptionService subscriptionService;
    private final OrganizationAccessGuard accessGuard;

    public UsageService(UsageDailyRepository usageDailyRepository,
                        DocumentRepository documentRepository,
                        SubscriptionService subscriptionService,
                        OrganizationAccessGuard accessGuard) {
        this.usageDailyRepository = usageDailyRepository;
        this.documentRepository = documentRepository;
        this.subscriptionService = subscriptionService;
        this.accessGuard = accessGuard;
    }

    /**
     * Atomically checks the daily/monthly AI quota and, if there's room, reserves one
     * request against today's count in the same row-locked transaction. Call this instead
     * of a separate check-then-increment around anything that isn't instantaneous (e.g. an
     * LLM call): checking and incrementing as two separate steps leaves a window where
     * concurrent requests can all pass the check before any of them commits, letting the
     * quota be exceeded.
     */
    @Transactional
    public void reserveAiRequest(UUID organizationId) {
        Plan plan = subscriptionService.getActivePlan(organizationId);
        LocalDate today = LocalDate.now();

        UsageDaily usage = lockedTodayUsage(organizationId, today);
        if (usage.getAiRequests() >= plan.getDailyAiQuestionLimit()) {
            throw new QuotaExceededException(
                    "Daily AI question limit reached (" + plan.getDailyAiQuestionLimit() + "). Try again tomorrow or upgrade your plan.");
        }

        long requestsThisMonth = sumAiRequestsForMonth(organizationId, YearMonth.now());
        if (requestsThisMonth >= plan.getMonthlyAiQuestionLimit()) {
            throw new QuotaExceededException(
                    "Monthly AI question limit reached (" + plan.getMonthlyAiQuestionLimit() + "). Upgrade your plan to continue.");
        }

        usage.setAiRequests(usage.getAiRequests() + 1);
        usageDailyRepository.save(usage);
    }

    /** Adds token usage to today's row after the actual request completes. Doesn't gate anything. */
    @Transactional
    public void addTokensUsed(UUID organizationId, long tokensUsed) {
        UsageDaily usage = lockedTodayUsage(organizationId, LocalDate.now());
        usage.setTokensUsed(usage.getTokensUsed() + tokensUsed);
        usageDailyRepository.save(usage);
    }

    public void assertStorageQuotaAvailable(UUID organizationId, long incomingFileSize) {
        Plan plan = subscriptionService.getActivePlan(organizationId);
        long currentUsage = currentStorageUsedBytes(organizationId);

        if (currentUsage + incomingFileSize > plan.getStorageLimitBytes()) {
            throw new StorageQuotaExceededException(
                    "Storage limit reached (" + (plan.getStorageLimitBytes() / (1024 * 1024)) + " MB). Delete files or upgrade your plan.");
        }
    }
    @Transactional(readOnly = true)
    public UsageResponse getUsageSummary(UUID currentUserId, UUID organizationId) {
        accessGuard.requireMembership(organizationId, currentUserId);

        Plan plan = subscriptionService.getActivePlan(organizationId);
        LocalDate today = LocalDate.now();

        long requestsToday = todayUsage(organizationId, today).getAiRequests();
        long requestsThisMonth = sumAiRequestsForMonth(organizationId, YearMonth.now());
        long tokensThisMonth = sumTokensForMonth(organizationId, YearMonth.now());
        long storageUsed = currentStorageUsedBytes(organizationId);
        long documentsUploaded = documentRepository.countByOrganizationIdAndDeletedFalse(organizationId);

        return new UsageResponse(
                requestsToday, requestsThisMonth, tokensThisMonth, storageUsed, documentsUploaded,
                plan.getDailyAiQuestionLimit(), plan.getMonthlyAiQuestionLimit(), plan.getStorageLimitBytes());
    }


    private UsageDaily todayUsage(UUID organizationId, LocalDate date) {
        return usageDailyRepository.findByOrganizationIdAndUsageDate(organizationId, date)
                .orElseGet(() -> {
                    UsageDaily fresh = new UsageDaily();
                    fresh.setOrganizationId(organizationId);
                    fresh.setUsageDate(date);
                    return usageDailyRepository.save(fresh);
                });
    }

    /**
     * Same as todayUsage() but takes a row lock on an existing row (via
     * findByOrganizationIdAndUsageDateForUpdate) so the caller can safely check-then-increment
     * it. If no row exists yet for today, creates one; a unique constraint on
     * (organization_id, usage_date) means a concurrent first request for the same org/day can
     * lose that race, so we catch the constraint violation and re-fetch (now with a lock)
     * instead of letting it surface as a 500.
     */
    private UsageDaily lockedTodayUsage(UUID organizationId, LocalDate date) {
        return usageDailyRepository.findByOrganizationIdAndUsageDateForUpdate(organizationId, date)
                .orElseGet(() -> {
                    try {
                        UsageDaily fresh = new UsageDaily();
                        fresh.setOrganizationId(organizationId);
                        fresh.setUsageDate(date);
                        usageDailyRepository.save(fresh);
                        usageDailyRepository.flush();
                        return fresh;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return usageDailyRepository.findByOrganizationIdAndUsageDateForUpdate(organizationId, date)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private long sumAiRequestsForMonth(UUID organizationId, YearMonth month) {
        List<UsageDaily> rows = usageDailyRepository.findInRange(
                organizationId, month.atDay(1), month.atEndOfMonth());
        return rows.stream().mapToLong(UsageDaily::getAiRequests).sum();
    }

    private long sumTokensForMonth(UUID organizationId, YearMonth month) {
        List<UsageDaily> rows = usageDailyRepository.findInRange(
                organizationId, month.atDay(1), month.atEndOfMonth());
        return rows.stream().mapToLong(UsageDaily::getTokensUsed).sum();
    }

    private long currentStorageUsedBytes(UUID organizationId) {
        // Sums file_size across all non-deleted documents for the org.
        // Simple enough at V1 scale; if this becomes a hot path at larger
        // scale, replace with a native SUM() query instead of loading rows.
        return documentRepository.findByOrganizationIdAndDeletedFalse(organizationId,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .mapToLong(com.verbamind.document.entity.Document::getFileSize)
                .sum();
    }
}