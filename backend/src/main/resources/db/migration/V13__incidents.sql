CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    execution_id UUID,
    anomaly_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    metadata JSONB,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_incidents_organization_created
    ON incidents (organization_id, created_at DESC);

CREATE INDEX idx_incidents_organization_status
    ON incidents (organization_id, status);

CREATE INDEX idx_incidents_organization_workflow
    ON incidents (organization_id, workflow_id);

CREATE INDEX idx_incidents_organization_anomaly
    ON incidents (organization_id, anomaly_id);
