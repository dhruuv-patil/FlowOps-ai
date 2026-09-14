-- FlowOps M5 (slice 2) — inbound secure webhooks.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always.
--
-- A `webhook` is a public, JWT-less ingress that lets an external system start a run
-- of ONE workflow:  POST /api/webhooks/{workflow_id}/{token}. The secret token in the
-- URL *is* the authentication; there is no principal on that path. Only the token's
-- SHA-256 hash (lowercase hex) is stored here — NEVER the token itself — plus a
-- non-secret last-4 `token_hint` for display. The full working URL (token embedded)
-- is shown to the admin exactly once, at generate time, and can never be recovered
-- from this row. High-entropy token (256 bits) ⇒ a fast one-way hash + constant-time
-- compare is the correct store/verify; bcrypt would add per-request cost for no gain.

CREATE TABLE webhooks (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    -- One webhook per workflow (upsert on regenerate). Deleting the workflow removes it.
    workflow_id     uuid         NOT NULL UNIQUE REFERENCES workflows (id) ON DELETE CASCADE,
    -- Lowercase hex SHA-256 of the token. Exactly 64 chars. Never the token itself.
    token_hash      char(64)     NOT NULL,
    -- Non-secret display tail (last 4 chars of the token). Safe to render.
    token_hint      varchar(8),
    enabled         boolean      NOT NULL,
    -- The admin who generated it; nullable and SET NULL on user delete — the webhook
    -- is owned by the organization, not the person who minted its token.
    created_by      uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);

-- Management listing/lookup is org-scoped; the UNIQUE(workflow_id) above already
-- indexes the public ingress lookup by workflow id.
CREATE INDEX ix_webhooks_org ON webhooks (organization_id);
