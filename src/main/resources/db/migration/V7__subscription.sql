CREATE TABLE plans (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       code VARCHAR(50) NOT NULL UNIQUE,          -- FREE | PRO | ENTERPRISE
                       name VARCHAR(100) NOT NULL,
                       storage_limit_bytes BIGINT NOT NULL,
                       daily_ai_question_limit INT NOT NULL,
                       monthly_ai_question_limit INT NOT NULL,
                       max_upload_size_bytes BIGINT NOT NULL,
                       price_monthly_paise BIGINT NOT NULL DEFAULT 0,  -- Razorpay uses paise (INR minor unit)
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at TIMESTAMP NOT NULL DEFAULT now(),
                       deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               organization_id UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
                               plan_id UUID NOT NULL REFERENCES plans(id),
                               status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CANCELED | PAST_DUE
                               current_period_start TIMESTAMP NOT NULL DEFAULT now(),
                               current_period_end TIMESTAMP,
                               cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at TIMESTAMP NOT NULL DEFAULT now(),
                               deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_subscriptions_organization_id ON subscriptions(organization_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);

INSERT INTO plans (id, code, name, storage_limit_bytes, daily_ai_question_limit, monthly_ai_question_limit, max_upload_size_bytes, price_monthly_paise)
VALUES
    (uuid_generate_v4(), 'FREE', 'Free', 1073741824, 20, 200, 26214400, 0),
    (uuid_generate_v4(), 'PRO', 'Pro', 10737418240, 200, 3000, 26214400, 99900),
    (uuid_generate_v4(), 'ENTERPRISE', 'Enterprise', 107374182400, 2000, 50000, 26214400, 499900);