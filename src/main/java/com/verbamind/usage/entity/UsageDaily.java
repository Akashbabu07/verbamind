package com.verbamind.usage.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usage_daily")
public class UsageDaily {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "ai_requests", nullable = false)
    private int aiRequests = 0;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed = 0;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }
    public int getAiRequests() { return aiRequests; }
    public void setAiRequests(int aiRequests) { this.aiRequests = aiRequests; }
    public long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(long tokensUsed) { this.tokensUsed = tokensUsed; }
}