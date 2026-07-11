CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                          plan_id UUID NOT NULL REFERENCES plans(id),
                          razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
                          razorpay_payment_id VARCHAR(255),
                          razorpay_signature VARCHAR(255),
                          amount_paise BIGINT NOT NULL,
                          currency VARCHAR(10) NOT NULL DEFAULT 'INR',
                          status VARCHAR(50) NOT NULL DEFAULT 'CREATED',  -- CREATED | PAID | FAILED
                          created_at TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at TIMESTAMP NOT NULL DEFAULT now(),
                          deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_payments_organization_id ON payments(organization_id);
CREATE INDEX idx_payments_razorpay_order_id ON payments(razorpay_order_id);