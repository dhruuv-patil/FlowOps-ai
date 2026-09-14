-- FlowOps M4 — AI agents: org-scoped, reusable AI configurations.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always. An agent's `tools` is a JSON array of allowlisted tool
-- names the AI service is permitted to invoke for this agent (never arbitrary code).
-- The provider API key is NOT stored here — it lives only on the AI service.

CREATE TABLE ai_agents (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            varchar(120) NOT NULL,
    description     varchar(500),
    -- System instructions that steer the agent. Never null.
    instructions    text         NOT NULL,
    -- Optional model override; null means "use the AI service default".
    model           varchar(80),
    -- Allowlisted tool names, e.g. ["get_current_time","calculate"]. Never null.
    tools           jsonb        NOT NULL DEFAULT '[]',
    created_by      uuid         NOT NULL REFERENCES users (id),
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);

CREATE INDEX ix_ai_agents_org ON ai_agents (organization_id);
-- Listing is always org-scoped and ordered by recency.
CREATE INDEX ix_ai_agents_org_updated ON ai_agents (organization_id, updated_at DESC);
