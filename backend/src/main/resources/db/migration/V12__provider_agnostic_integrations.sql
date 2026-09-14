-- FlowOps provider-agnostic integrations — external workflows, sync state, and relaxed telemetry FKs.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always. Every table carries organization_id for tenant isolation,
-- and every cross-tenant access path filters on it (repositories never expose a
-- bare findById to callers).
--
-- This migration is ADDITIVE to V1–V11. It does not delete or rename any
-- existing columns/tables. Existing Slack integrations and internal execution
-- paths continue to work without modification.

-- =========================================================================
-- 1) Extend integrations with generic metadata (non-secret provider config)
-- =========================================================================
ALTER TABLE integrations
    ADD COLUMN metadata jsonb;

-- Slack rows keep NULL metadata (or a future baseUrl) — no data migration needed.

-- =========================================================================
-- 2) integration_workflows — maps (integration × remote workflow) → FlowOps
-- =========================================================================
CREATE TABLE integration_workflows (
    id                     uuid          PRIMARY KEY,
    organization_id        uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    integration_id         uuid          NOT NULL REFERENCES integrations (id) ON DELETE CASCADE,
    -- Nullable: only set when the user later creates a FlowOps-native workflow
    -- representation. Null for the light path (n8n workflow → telemetry directly).
    workflow_id            uuid          REFERENCES workflows (id) ON DELETE SET NULL,
    provider_workflow_id   varchar(255)  NOT NULL,
    name                   varchar(500),
    description            text,
    -- Remote status (e.g. "active" / "inactive"), display-only.
    status                 varchar(50),
    monitoring_enabled     boolean       NOT NULL DEFAULT FALSE,
    metadata               jsonb,
    last_synced_at         timestamptz,
    created_at             timestamptz   NOT NULL,
    updated_at             timestamptz   NOT NULL
);

-- One remote workflow per integration (prevents duplicate mapping + double-selection).
CREATE UNIQUE INDEX ux_integration_workflow_external
    ON integration_workflows (integration_id, provider_workflow_id);

-- Org-scoped listing, newest first.
CREATE INDEX ix_integration_workflows_org_updated
    ON integration_workflows (organization_id, updated_at DESC);

-- =========================================================================
-- 3) integration_sync_state — one row per integration
-- =========================================================================
CREATE TABLE integration_sync_state (
    id                     uuid          PRIMARY KEY,
    organization_id        uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    integration_id         uuid          NOT NULL UNIQUE REFERENCES integrations (id) ON DELETE CASCADE,
    cursor                 varchar(1000),                   -- provider cursor (n8n: last execution id → afterId)
    last_synced_at         timestamptz,
    last_success_at        timestamptz,
    last_error_at          timestamptz,
    last_error             text,
    status                 varchar(20)   NOT NULL DEFAULT 'NEW',   -- NEW | HEALTHY | FAILED
    attempt                integer       NOT NULL DEFAULT 0,       -- bounded backoff counter
    created_at             timestamptz   NOT NULL,
    updated_at             timestamptz   NOT NULL
);

-- Org-scoped listing.
CREATE INDEX ix_integration_sync_state_org
    ON integration_sync_state (organization_id);

-- =========================================================================
-- 4) Add integration_id to execution_events
-- =========================================================================
ALTER TABLE execution_events
    ADD COLUMN integration_id uuid REFERENCES integrations (id) ON DELETE CASCADE;

-- Idempotency key: one event per (integration, workflow, execution, step).
-- Existing rows have NULL integration_id; unique index with NULLs is permissive
-- in Postgres, so pre-existing/custom-injected rows don't collide.
-- New provider-synced rows always carry integration_id.
CREATE UNIQUE INDEX ux_execution_events_external_identity
    ON execution_events (integration_id, workflow_external_id, execution_external_id, step_external_id);

-- Common query paths.
CREATE INDEX ix_execution_events_integration
    ON execution_events (integration_id, created_at DESC);
CREATE INDEX ix_execution_events_integration_workflow
    ON execution_events (integration_id, workflow_external_id, created_at DESC);

-- =========================================================================
-- 5) Relax FKs on telemetry/baselines/anomalies so external data can key on
--    integration_workflows.id / synthetic execution UUIDs instead of the
--    internal workflows/workflow_executions tables.
--
-- IMPORTANT: Hibernate `ddl-auto=validate` only checks column types/names,
-- NOT the presence of FK constraints. Internal paths still write real
-- workflow/execution UUIDs; the constraints are simply no longer enforced at
-- the DB level, so external rows are accepted. Org-scoped indexes + application-
-- layer checks remain.
-- =========================================================================

-- node_execution_metrics
ALTER TABLE node_execution_metrics
    DROP CONSTRAINT IF EXISTS node_execution_metrics_workflow_id_fkey,
    DROP CONSTRAINT IF EXISTS node_execution_metrics_execution_id_fkey;

-- metric_baselines
ALTER TABLE metric_baselines
    DROP CONSTRAINT IF EXISTS metric_baselines_workflow_id_fkey;

-- anomalies
ALTER TABLE anomalies
    DROP CONSTRAINT IF EXISTS anomalies_workflow_id_fkey,
    DROP CONSTRAINT IF EXISTS anomalies_execution_id_fkey;

-- execution_events (the V11 FK on workflow_id)
ALTER TABLE execution_events
    DROP CONSTRAINT IF EXISTS execution_events_workflow_id_fkey;

-- Keep the columns; add org-scoped indexes for query performance on external paths.
CREATE INDEX IF NOT EXISTS ix_node_metrics_org_workflow
    ON node_execution_metrics (organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS ix_metric_baselines_org_workflow
    ON metric_baselines (organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS ix_anomalies_org_workflow
    ON anomalies (organization_id, workflow_id);
CREATE INDEX IF NOT EXISTS ix_execution_events_org_workflow
    ON execution_events (organization_id, workflow_id);