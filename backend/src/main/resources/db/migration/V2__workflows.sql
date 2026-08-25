-- FlowOps M2 — workflows, immutable versions, and execution-ready graph storage.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. Graphs are stored as jsonb: the
-- working copy on `workflows.draft_graph`, and an immutable snapshot per publish
-- on `workflow_versions.graph`. UUIDs are generated in Java. TIMESTAMPTZ always.

CREATE TABLE workflows (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            varchar(120) NOT NULL,
    description     varchar(500),
    status          varchar(16)  NOT NULL DEFAULT 'DRAFT',
    -- The editable working graph. Never null: a new workflow starts with an empty
    -- {nodes:[],edges:[]} document so the builder always has something to load.
    draft_graph     jsonb        NOT NULL,
    -- Points at the highest published version_number, or null if never published.
    latest_version  integer,
    created_by      uuid         NOT NULL REFERENCES users (id),
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    CONSTRAINT ck_workflows_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX ix_workflows_org ON workflows (organization_id);
-- Listing is always org-scoped and ordered by recency.
CREATE INDEX ix_workflows_org_updated ON workflows (organization_id, updated_at DESC);

-- One immutable row per publish. The graph is snapshotted so a running execution
-- (M3) always binds to the exact version it started on, even as the draft evolves.
CREATE TABLE workflow_versions (
    id             uuid        PRIMARY KEY,
    workflow_id    uuid        NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    version_number integer     NOT NULL,
    graph          jsonb       NOT NULL,
    -- Optional human note captured at publish time.
    note           varchar(500),
    published_by   uuid        NOT NULL REFERENCES users (id),
    created_at     timestamptz NOT NULL,
    CONSTRAINT uq_workflow_versions_number UNIQUE (workflow_id, version_number)
);

CREATE INDEX ix_workflow_versions_workflow ON workflow_versions (workflow_id, version_number DESC);
