CREATE TABLE offers (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    seller_user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT offers_one_per_seller_per_task_unique UNIQUE (task_id, seller_user_id),
    CONSTRAINT offers_amount_positive CHECK (amount > 0),
    CONSTRAINT offers_currency_supported CHECK (currency = 'USD'),
    CONSTRAINT offers_status_supported CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX offers_task_created_at_idx ON offers (task_id, created_at DESC);
CREATE INDEX offers_seller_created_at_idx ON offers (seller_user_id, created_at DESC);
CREATE INDEX offers_task_status_idx ON offers (task_id, status);
