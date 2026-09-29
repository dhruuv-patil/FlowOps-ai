-- Recovery verification fields added to anomalies (recovery-verification feature).
--
-- These columns track the in-progress evidence window when a user triggers
-- "Verify Recovery". Nullable so existing rows are unaffected (no backfill needed).
--
-- recovery_started_at         : when verification was initiated
-- recovery_healthy_count      : healthy (in-baseline) executions seen so far
-- recovery_observed_count     : total executions observed since verification start
-- recovery_required_count     : healthy executions needed for auto-resolve (default 5)
-- recovery_seen_executions    : JSON array of execution UUIDs already counted
--                               (idempotency: prevents a multi-node execution from
--                               counting as multiple observations)
--
-- The status CHECK constraint is extended to include VERIFYING_RECOVERY.

ALTER TABLE anomalies
    ADD COLUMN recovery_started_at         TIMESTAMPTZ,
    ADD COLUMN recovery_healthy_count      INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN recovery_observed_count     INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN recovery_required_count     INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN recovery_seen_executions    JSONB;

-- Drop and re-add the CHECK constraint so VERIFYING_RECOVERY is valid.
ALTER TABLE anomalies
    DROP CONSTRAINT IF EXISTS ck_anomalies_status;

ALTER TABLE anomalies
    ADD CONSTRAINT ck_anomalies_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'VERIFYING_RECOVERY', 'RESOLVED', 'FALSE_POSITIVE'));

-- Index to efficiently find anomalies that are actively being verified
-- (the recovery sweep needs to scan these on every execution completion).
CREATE INDEX ix_anomalies_verifying
    ON anomalies (workflow_id, status)
    WHERE status = 'VERIFYING_RECOVERY';
