package com.verbamind.admin.service;

import com.verbamind.admin.dto.AiUsageAnalyticsResponse;
import com.verbamind.document.repository.DocumentRepository;
import com.verbamind.organization.repository.OrganizationRepository;
import com.verbamind.usage.repository.UsageDailyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class AdminAnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final UsageDailyRepository usageDailyRepository;
    private final DocumentRepository documentRepository;

    public AdminAnalyticsService(OrganizationRepository organizationRepository,
                                 UsageDailyRepository usageDailyRepository,
                                 DocumentRepository documentRepository) {
        this.organizationRepository = organizationRepository;
        this.usageDailyRepository = usageDailyRepository;
        this.documentRepository = documentRepository;
    }

    public AiUsageAnalyticsResponse getAnalytics() {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        long totalOrgs = organizationRepository.count();
        long requestsToday = usageDailyRepository.sumAiRequestsForDate(today);
        long requestsThisMonth = usageDailyRepository.sumAiRequestsInRange(thisMonth.atDay(1), thisMonth.atEndOfMonth());
        long tokensThisMonth = usageDailyRepository.sumTokensInRange(thisMonth.atDay(1), thisMonth.atEndOfMonth());
        long totalDocuments = documentRepository.countByDeletedFalse();
        long totalStorage = documentRepository.sumFileSizeAllOrganizations();

        return new AiUsageAnalyticsResponse(totalOrgs, requestsToday, requestsThisMonth,
                tokensThisMonth, totalDocuments, totalStorage);
    }
}