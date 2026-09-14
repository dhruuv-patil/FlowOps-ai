-- FlowOps — password reset tokens (M5 slice: auth hardening)
--
-- Single-use, short-lived reset tokens. Only the SHA-256 hash is stored.
-- TTL: 1 hour (configurable via flowops.auth.reset-token-ttl).

CREATE TABLE password_reset_tokens (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash      varchar(64) NOT NULL,
    expires_at      timestamptz NOT NULL,
    created_at      timestamptz NOT NULL,
    used_at         timestamptz,
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id);