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

    public void assertAiQuotaAvailable(UUID organizationId) {
        Plan plan = subscriptionService.getActivePlan(organizationId);
        LocalDate today = LocalDate.now();

        long requestsToday = todayUsage(organizationId, today).getAiRequests();
        if (requestsToday >= plan.getDailyAiQuestionLimit()) {
            throw new QuotaExceededException(
                    "Daily AI question limit reached (" + plan.getDailyAiQuestionLimit() + "). Try again tomorrow or upgrade your plan.");
        }

        long requestsThisMonth = sumAiRequestsForMonth(organizationId, YearMonth.now());
        if (requestsThisMonth >= plan.getMonthlyAiQuestionLimit()) {
            throw new QuotaExceededException(
                    "Monthly AI question limit reached (" + plan.getMonthlyAiQuestionLimit() + "). Upgrade your plan to continue.");
        }
    }

    public void assertStorageQuotaAvailable(UUID organizationId, long incomingFileSize) {
        Plan plan = subscriptionService.getActivePlan(organizationId);
        long currentUsage = currentStorageUsedBytes(organizationId);

        if (currentUsage + incomingFileSize > plan.getStorageLimitBytes()) {
            throw new StorageQuotaExceededException(
                    "Storage limit reached (" + (plan.getStorageLimitBytes() / (1024 * 1024)) + " MB). Delete files or upgrade your plan.");
        }
    }
    @Transactional
    public void recordAiRequest(UUID organizationId, long tokensUsed) {
        LocalDate today = LocalDate.now();
        UsageDaily usage = todayUsage(organizationId, today);
        usage.setAiRequests(usage.getAiRequests() + 1);
        usage.setTokensUsed(usage.getTokensUsed() + tokensUsed);
        usageDailyRepository.save(usage);
    }


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