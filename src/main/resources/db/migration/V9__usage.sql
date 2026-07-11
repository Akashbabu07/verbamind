-- One row per organization per calendar day, for daily AI question limits
-- and rolling analytics. Monthly totals are derived by summing daily rows.
CREATE TABLE usage_daily (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                             usage_date DATE NOT NULL,
                             ai_requests INT NOT NULL DEFAULT 0,
                             tokens_used BIGINT NOT NULL DEFAULT 0,
                             created_at TIMESTAMP NOT NULL DEFAULT now(),
                             updated_at TIMESTAMP NOT NULL DEFAULT now(),
                             CONSTRAINT uq_usage_daily_org_date UNIQUE (organization_id, usage_date)
);

CREATE INDEX idx_usage_daily_organization_id ON usage_daily(organization_id);
CREATE INDEX idx_usage_daily_usage_date ON usage_daily(usage_date);