package com.verbamind.usage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usage_daily")
public class UsageDaily {

    @Getter
    @Id
    @GeneratedValue
    private UUID id;

    @Setter
    @Getter
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Getter
    @Setter
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Getter
    @Setter
    @Column(name = "ai_requests", nullable = false)
    private int aiRequests = 0;

    @Setter
    @Getter
    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed = 0;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false)
    private Instant updatedAt;

}