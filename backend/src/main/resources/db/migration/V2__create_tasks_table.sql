CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    budget_amount NUMERIC(12, 2) NOT NULL,
    budget_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT tasks_budget_amount_positive CHECK (budget_amount > 0),
    CONSTRAINT tasks_budget_currency_supported CHECK (budget_currency = 'USD'),
    CONSTRAINT tasks_status_supported CHECK (status = 'OPEN')
);

CREATE INDEX tasks_status_created_at_idx ON tasks (status, created_at DESC);
CREATE INDEX tasks_owner_user_id_idx ON tasks (owner_user_id);
