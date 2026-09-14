-- FlowOps M5 (slice 1) — integrations & their encrypted credentials.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always.
--
-- An `integration` is an org's connection to an external provider (Slack in M5).
-- Its secret (a Slack incoming-webhook URL) is AES-256-GCM encrypted and stored in
-- `integration_credentials.ciphertext` — NEVER in plaintext, NEVER returned to the
-- client. The non-secret display `hint` (last-4) lives on the integration row so the
-- API can render "connected ••••1234" without ever reading the ciphertext table; the
-- ciphertext is read only by the execution engine at run time to deliver a message.

CREATE TABLE integrations (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    -- Provider discriminator, e.g. 'slack'. Lowercase, stable identifier.
    type            varchar(40)  NOT NULL,
    name            varchar(120) NOT NULL,
    -- 'connected' | 'disconnected'. A disconnected row keeps no credential.
    status          varchar(20)  NOT NULL,
    -- Non-secret display tail (last 4 chars of the secret). Null when disconnected.
    hint            varchar(24),
    created_by      uuid         NOT NULL REFERENCES users (id),
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    -- One connection per provider per org in M5 (upsert on reconnect).
    CONSTRAINT uq_integrations_org_type UNIQUE (organization_id, type)
);

-- Listing is always org-scoped and ordered by recency.
CREATE INDEX ix_integrations_org_updated ON integrations (organization_id, updated_at DESC);

CREATE TABLE integration_credentials (
    id              uuid         PRIMARY KEY,
    -- 1:1 with an integration; deleted when the integration is disconnected.
    integration_id  uuid         NOT NULL UNIQUE REFERENCES integrations (id) ON DELETE CASCADE,
    -- AES-256-GCM blob: base64( iv ‖ ciphertext ‖ tag ). Opaque; never logged.
    ciphertext      text         NOT NULL,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);
