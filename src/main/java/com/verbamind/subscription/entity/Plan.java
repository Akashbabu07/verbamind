package com.verbamind.subscription.entity;

import com.verbamind.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PlanCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "storage_limit_bytes", nullable = false)
    private long storageLimitBytes;

    @Column(name = "daily_ai_question_limit", nullable = false)
    private int dailyAiQuestionLimit;

    @Column(name = "monthly_ai_question_limit", nullable = false)
    private int monthlyAiQuestionLimit;

    @Column(name = "max_upload_size_bytes", nullable = false)
    private long maxUploadSizeBytes;

    @Column(name = "price_monthly_paise", nullable = false)
    private long priceMonthlyPaise;

    @Column(nullable = false)
    private boolean active = true;

    public PlanCode getCode() { return code; }
    public void setCode(PlanCode code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getStorageLimitBytes() { return storageLimitBytes; }
    public void setStorageLimitBytes(long storageLimitBytes) { this.storageLimitBytes = storageLimitBytes; }

    public int getDailyAiQuestionLimit() { return dailyAiQuestionLimit; }
    public void setDailyAiQuestionLimit(int dailyAiQuestionLimit) { this.dailyAiQuestionLimit = dailyAiQuestionLimit; }

    public int getMonthlyAiQuestionLimit() { return monthlyAiQuestionLimit; }
    public void setMonthlyAiQuestionLimit(int monthlyAiQuestionLimit) { this.monthlyAiQuestionLimit = monthlyAiQuestionLimit; }

    public long getMaxUploadSizeBytes() { return maxUploadSizeBytes; }
    public void setMaxUploadSizeBytes(long maxUploadSizeBytes) { this.maxUploadSizeBytes = maxUploadSizeBytes; }

    public long getPriceMonthlyPaise() { return priceMonthlyPaise; }
    public void setPriceMonthlyPaise(long priceMonthlyPaise) { this.priceMonthlyPaise = priceMonthlyPaise; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}