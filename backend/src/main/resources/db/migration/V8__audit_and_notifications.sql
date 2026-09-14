-- FlowOps M5 (slice 4) — audit trail and in-app notifications.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always.
--
-- An `audit_log` row is an append-only record of a security-relevant action taken
-- inside one organization: membership and role changes, invitations, credential
-- connect/disconnect, webhook token rotation, password changes, and failed webhook
-- authentication. Rows are NEVER updated or deleted by the application — the table
-- is the tenant's own tamper-evidence surface, readable only by an admin.
--
-- Secrets never reach this table. `summary` carries a human-readable sentence built
-- from non-secret facts only (role names, integration types, masked last-4 hints);
-- tokens, webhook URLs and passwords are excluded by construction at the call site.
--
-- A `notification` is an in-app message addressed to ONE user within ONE
-- organization. It is derived from real state (a run that failed, a run parked on a
-- human approval) — there is no seeded or synthetic notification anywhere.

CREATE TABLE audit_logs (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    -- The acting user. Nullable because some audited events have no principal at
    -- all: a failed inbound-webhook authentication is anonymous by design.
    -- SET NULL on user delete so history survives the account.
    actor_user_id   uuid         REFERENCES users (id) ON DELETE SET NULL,
    -- Denormalized snapshot of the actor's email at the time of the action, so the
    -- trail stays readable after the account is renamed or deleted. Null for
    -- anonymous events.
    actor_email     varchar(255),
    -- Stable machine name of the action (see the AuditAction enum). Filterable.
    action          varchar(64)  NOT NULL,
    -- What the action was performed on: 'member' | 'invitation' | 'integration' |
    -- 'webhook' | 'organization' | 'user'. Free-form but small and stable.
    target_type     varchar(40),
    -- Identifier of the target as text (a UUID, an email, or a workflow id). Text
    -- rather than uuid because the target is not always a UUID.
    target_id       varchar(200),
    -- Non-secret human-readable sentence. Never contains a credential.
    summary         varchar(500),
    -- Client IP when the action arrived over HTTP and the address was available.
    ip              varchar(64),
    created_at      timestamptz  NOT NULL
);

-- The management view is always "this org, newest first", optionally filtered by action.
CREATE INDEX ix_audit_logs_org_created ON audit_logs (organization_id, created_at DESC);
CREATE INDEX ix_audit_logs_org_action ON audit_logs (organization_id, action, created_at DESC);

CREATE TABLE notifications (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    -- The addressee. Deleting the user removes their notifications outright: they
    -- are a personal inbox, not organizational history (that is audit_logs above).
    user_id         uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    level           varchar(8)   NOT NULL,
    title           varchar(200) NOT NULL,
    body            varchar(500),
    -- Relative in-app path the notification links to (e.g. /executions/{id}).
    -- Never an absolute URL, so it can never point off-origin.
    link            varchar(300),
    -- NULL until the addressee reads it. The only mutable column on this table.
    read_at         timestamptz,
    created_at      timestamptz  NOT NULL,
    CONSTRAINT ck_notifications_level
        CHECK (level IN ('INFO', 'WARN', 'ERROR'))
);

-- The inbox listing is "my notifications, newest first".
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
-- The unread badge counts only unread rows; a partial index keeps that count cheap.
CREATE INDEX ix_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
