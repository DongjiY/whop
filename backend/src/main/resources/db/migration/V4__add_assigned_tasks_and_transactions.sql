ALTER TABLE tasks DROP CONSTRAINT tasks_status_supported;
ALTER TABLE tasks ADD CONSTRAINT tasks_status_supported CHECK (status IN ('OPEN', 'ASSIGNED'));

ALTER TABLE tasks ADD COLUMN accepted_offer_id UUID NULL REFERENCES offers(id);
CREATE UNIQUE INDEX tasks_accepted_offer_id_unique_idx
    ON tasks (accepted_offer_id)
    WHERE accepted_offer_id IS NOT NULL;

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    offer_id UUID NOT NULL UNIQUE REFERENCES offers(id),
    buyer_user_id UUID NOT NULL REFERENCES users(id),
    seller_user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'RECORDED',
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT transactions_currency_supported CHECK (currency = 'USD'),
    CONSTRAINT transactions_status_supported CHECK (status = 'RECORDED')
);

CREATE INDEX transactions_buyer_recorded_at_idx ON transactions (buyer_user_id, recorded_at DESC);
CREATE INDEX transactions_seller_recorded_at_idx ON transactions (seller_user_id, recorded_at DESC);
CREATE INDEX transactions_task_id_idx ON transactions (task_id);
