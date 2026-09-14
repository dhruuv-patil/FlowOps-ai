-- FlowOps reliability pivot — telemetry, baselines, and anomalies.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always. Every table carries organization_id for tenant isolation,
-- and every cross-tenant access path filters on it (repositories never expose a
-- bare findById to callers).
--
-- SECURITY: reliability telemetry stores SIZES and STRUCTURE, never values.
-- node_execution_metrics carries input_size/output_size (byte counts) and an
-- output_signature (a field-name -> type map), never raw inputs, outputs, headers,
-- or credentials. error_message is a sanitized, bounded user-safe string.

-- One normalized observation per executed node. Derived at workflow terminal state
-- from the existing execution_nodes / workflow_executions rows — no hot-path
-- instrumentation, no second event bus.
CREATE TABLE node_execution_metrics (
    id               uuid          PRIMARY KEY,
    organization_id  uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    workflow_id      uuid          NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    workflow_version integer       NOT NULL,
    execution_id     uuid          NOT NULL REFERENCES workflow_executions (id) ON DELETE CASCADE,
    node_id          varchar(128)  NOT NULL,
    node_type        varchar(64)   NOT NULL,
    status           varchar(16)   NOT NULL,          -- SUCCEEDED | FAILED | SKIPPED
    started_at       timestamptz,
    completed_at     timestamptz,
    duration_ms      bigint,
    retry_count      integer       NOT NULL DEFAULT 0,
    -- Byte lengths of the persisted input/output JSON, NOT the payloads themselves.
    input_size       integer,
    output_size      integer,
    -- User-safe, sanitized failure summary (never a stack trace / secret).
    error_type       varchar(64),
    error_message    text,
    -- Optional AI/provider fields, populated when the executor reports them.
    tokens_used      integer,
    estimated_cost   numeric(20,8),
    http_status      integer,
    response_size    integer,
    provider         varchar(64),
    model_name       varchar(80),
    -- Structural signature of the output (which fields and types), never values.
    output_signature jsonb,
    created_at       timestamptz   NOT NULL,
    updated_at       timestamptz   NOT NULL
);

CREATE INDEX ix_node_metrics_workflow_node ON node_execution_metrics (workflow_id, node_id, started_at DESC);
CREATE INDEX ix_node_metrics_workflow_started ON node_execution_metrics (workflow_id, started_at DESC);
CREATE INDEX ix_node_metrics_org_started ON node_execution_metrics (organization_id, started_at DESC);
CREATE INDEX ix_node_metrics_execution ON node_execution_metrics (execution_id);

-- Learned normal per (workflow, node, metric). node_id is '' (empty string) for
-- workflow-scoped metrics (volume, overall error rate) so the unique constraint
-- works uniformly — Postgres treats NULLs as distinct in a unique constraint.
CREATE TABLE metric_baselines (
    id              uuid          PRIMARY KEY,
    organization_id uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    workflow_id     uuid          NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    node_id         varchar(128)  NOT NULL DEFAULT '',  -- '' = workflow-scoped metric
    metric          varchar(32)   NOT NULL,  -- LATENCY | OUTPUT_SIZE | VOLUME | RETRY_RATE | ERROR_RATE | OUTPUT_SCHEMA | BEHAVIOR
    mean            double precision,
    median          double precision,
    stddev          double precision,
    p50             double precision,
    p95             double precision,
    p99             double precision,
    min             double precision,
    max             double precision,
    mad             double precision,          -- robust dispersion (median abs deviation)
    sample_count    integer       NOT NULL DEFAULT 0,
    window_start    timestamptz,
    window_end      timestamptz,
    -- Expected shape for OUTPUT_SCHEMA (field->type distribution) and BEHAVIOR
    -- (node-path signatures + frequencies). Structure only, never values.
    signature       jsonb,
    created_at      timestamptz   NOT NULL,
    updated_at      timestamptz   NOT NULL,
    CONSTRAINT uq_metric_baselines UNIQUE (workflow_id, node_id, metric)
);

CREATE INDEX ix_metric_baselines_org ON metric_baselines (organization_id);

-- Detected anomalies. Dedup/cooldown key so repeated anomalies aggregate into the
-- same record (bumping affected_executions) instead of spamming the dashboard.
CREATE TABLE anomalies (
    id                   uuid          PRIMARY KEY,
    organization_id      uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    workflow_id          uuid          NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    node_id              varchar(128),
    execution_id         uuid          REFERENCES workflow_executions (id) ON DELETE SET NULL,
    type                 varchar(32)   NOT NULL,  -- VOLUME | LATENCY | OUTPUT | BEHAVIORAL
    severity             varchar(16)   NOT NULL,  -- LOW | MEDIUM | HIGH | CRITICAL
    metric               varchar(64),
    expected_value       text,          -- human-readable, e.g. "p95 = 1.2s"
    actual_value         text,          -- human-readable, e.g. "9.8s"
    deviation            double precision,
    confidence           double precision,
    status               varchar(20)   NOT NULL DEFAULT 'OPEN',  -- OPEN|ACKNOWLEDGED|RESOLVED|FALSE_POSITIVE
    affected_executions  integer       NOT NULL DEFAULT 1,
    -- Explainable evidence: expected/observed/deviation, downstream impact, and the
    -- severity rationale. Never contains payload values or secrets.
    evidence             jsonb,
    dedup_key            varchar(128)  NOT NULL,
    detected_at          timestamptz   NOT NULL,
    updated_at           timestamptz   NOT NULL,
    created_at           timestamptz   NOT NULL,
    CONSTRAINT ck_anomalies_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_anomalies_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_POSITIVE'))
);

CREATE INDEX ix_anomalies_org_status ON anomalies (organization_id, status, detected_at DESC);
CREATE INDEX ix_anomalies_workflow ON anomalies (workflow_id, detected_at DESC);
CREATE INDEX ix_anomalies_dedup ON anomalies (dedup_key, detected_at DESC);

-- Idempotency keys for safe replay of externally-triggered executions (webhooks).
-- Keyed by composite "webhook:{workflowId}:{callerKey}" to prevent cross-workflow collisions.
CREATE TABLE idempotency_keys (
    id              uuid          PRIMARY KEY,
    key             varchar(256)  NOT NULL UNIQUE,
    execution_id    uuid          NOT NULL REFERENCES workflow_executions (id) ON DELETE CASCADE,
    expires_at      timestamptz   NOT NULL,
    created_at      timestamptz   NOT NULL
);

CREATE INDEX ix_idempotency_keys_expires ON idempotency_keys (expires_at);

-- Execution events ingested from external workflow systems (n8n, Zapier, Temporal, etc.).
-- Used for cross-system anomaly detection, health scoring, and observability.
CREATE TABLE execution_events (
    id                     uuid          PRIMARY KEY,
    organization_id        uuid          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    workflow_id            uuid          NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    source                 varchar(64)   NOT NULL,       -- n8n, zapier, temporal, custom, etc.
    workflow_external_id   varchar(256)  NOT NULL,       -- workflow ID in the source system
    execution_external_id  varchar(256)  NOT NULL,       -- execution/run ID in the source system
    step_external_id       varchar(256)  NOT NULL,       -- step/node ID in the source system
    step_name              varchar(256),
    status                 varchar(32)   NOT NULL,       -- RUNNING, SUCCEEDED, FAILED, WAITING, SKIPPED, etc.
    started_at             timestamptz,
    finished_at            timestamptz,
    duration_ms            bigint,
    retry_count            integer       NOT NULL DEFAULT 0,
    input_size             integer,
    output_size            integer,
    error_type             varchar(64),
    error_message          text,
    metadata               jsonb,
    created_at             timestamptz   NOT NULL
);

CREATE INDEX ix_execution_events_org_created ON execution_events (organization_id, created_at DESC);
CREATE INDEX ix_execution_events_workflow_created ON execution_events (workflow_id, created_at DESC);
CREATE INDEX ix_execution_events_external_exec ON execution_events (execution_external_id);
CREATE INDEX ix_execution_events_source ON execution_events (source);
