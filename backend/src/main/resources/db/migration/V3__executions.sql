-- FlowOps M3 — execution engine: runs, per-node state, and logs.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always. An execution binds to one immutable workflow_versions row,
-- so it is unaffected by later edits to the workflow's draft.

CREATE TABLE workflow_executions (
    id                  uuid         PRIMARY KEY,
    organization_id     uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    workflow_id         uuid         NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    -- The frozen version this run executes. No ON DELETE CASCADE from versions:
    -- versions are immutable and never deleted independently of their workflow.
    workflow_version_id uuid         NOT NULL REFERENCES workflow_versions (id),
    version_number      integer      NOT NULL,
    status              varchar(16)  NOT NULL DEFAULT 'QUEUED',
    trigger_type        varchar(16)  NOT NULL,
    -- The payload the run was started with (manual "input", or a webhook body in
    -- M5). Never null: an empty object when a run carried no input.
    trigger_payload     jsonb        NOT NULL,
    -- User-safe failure summary, set only on FAILED. Never a stack trace.
    error               text,
    -- Null for non-interactive triggers (e.g. a future webhook run).
    created_by          uuid         REFERENCES users (id),
    started_at          timestamptz,
    finished_at         timestamptz,
    created_at          timestamptz  NOT NULL,
    updated_at          timestamptz  NOT NULL,
    CONSTRAINT ck_executions_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'WAITING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_executions_trigger
        CHECK (trigger_type IN ('MANUAL', 'WEBHOOK'))
);

-- Listing is always org-scoped, newest first.
CREATE INDEX ix_executions_org_created ON workflow_executions (organization_id, created_at DESC);
-- The per-workflow "recent runs" panel.
CREATE INDEX ix_executions_workflow_created ON workflow_executions (workflow_id, created_at DESC);

-- One row per graph node, created up front as PENDING so the monitor can show the
-- whole plan. Branch nodes leave the untaken side SKIPPED. Unique per (run, node).
CREATE TABLE execution_nodes (
    id            uuid         PRIMARY KEY,
    execution_id  uuid         NOT NULL REFERENCES workflow_executions (id) ON DELETE CASCADE,
    node_id       varchar(128) NOT NULL,
    node_type     varchar(64)  NOT NULL,
    label         varchar(200),
    status        varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempt       integer      NOT NULL DEFAULT 0,
    -- The resolved inputs (active predecessors' outputs) and this node's output.
    input         jsonb,
    output        jsonb,
    -- Output ports that fired, comma-joined (e.g. "out", or "true"). Drives which
    -- downstream edges activate. Handle names never contain a comma.
    active_handles varchar(256),
    error         text,
    started_at    timestamptz,
    finished_at   timestamptz,
    duration_ms   bigint,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    CONSTRAINT uq_execution_nodes UNIQUE (execution_id, node_id),
    CONSTRAINT ck_execution_nodes_status
        CHECK (status IN ('PENDING', 'RUNNING', 'WAITING', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX ix_execution_nodes_execution ON execution_nodes (execution_id);

-- Append-only run log. `seq` is a per-run monotonic counter assigned by the engine
-- so the timeline is stable even when several lines share a millisecond.
CREATE TABLE execution_logs (
    id           uuid        PRIMARY KEY,
    execution_id uuid        NOT NULL REFERENCES workflow_executions (id) ON DELETE CASCADE,
    node_id      varchar(128),
    level        varchar(8)  NOT NULL,
    message      text        NOT NULL,
    seq          integer     NOT NULL,
    created_at   timestamptz NOT NULL,
    CONSTRAINT ck_execution_logs_level
        CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR'))
);

CREATE INDEX ix_execution_logs_execution ON execution_logs (execution_id, seq);
