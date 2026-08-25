-- FlowOps M1 — auth + multi-tenancy baseline (API contract §10).
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. All UUIDs are generated in Java, so
-- no server-side default is needed. TIMESTAMPTZ everywhere.
--
-- Deviation from the reference schema, deliberate: refresh_tokens.token_hash is
-- VARCHAR(64) rather than CHAR(64). CHAR is blank-padded in Postgres (a lookup
-- comparison surprise waiting to happen) and reports as `bpchar`, which trips
-- Hibernate's schema validator against a String mapping. The value is always
-- exactly 64 lowercase hex chars either way.

CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(255) NOT NULL,
    password_hash varchar(72)  NOT NULL,   -- BCrypt strength 12 (60 chars + headroom)
    full_name     varchar(100) NOT NULL,
    avatar_url    varchar(512),
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

-- Email uniqueness is case-insensitive and enforced by the database: register
-- translates the constraint violation into 409 EMAIL_ALREADY_REGISTERED instead
-- of racing a check-then-insert.
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE organizations (
    id         uuid         PRIMARY KEY,
    name       varchar(80)  NOT NULL,   -- as typed, NOT unique across tenants
    slug       varchar(100) NOT NULL UNIQUE,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL
);

CREATE TABLE organization_members (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    organization_id uuid        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    role            varchar(16) NOT NULL,
    joined_at       timestamptz NOT NULL,
    CONSTRAINT uq_organization_members_user_org UNIQUE (user_id, organization_id),
    CONSTRAINT ck_organization_members_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX ix_organization_members_user ON organization_members (user_id);
CREATE INDEX ix_organization_members_org ON organization_members (organization_id);

-- One row per login. Survives refresh-token rotation, and carries the switchable
-- current organization so a switched tenant survives a page reload (the `sid`
-- claim is this row's id).
CREATE TABLE auth_sessions (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    organization_id uuid        NOT NULL REFERENCES organizations (id),
    created_at      timestamptz NOT NULL,
    last_used_at    timestamptz NOT NULL,
    revoked_at      timestamptz,
    user_agent      varchar(255),
    ip              varchar(45)
);

CREATE INDEX ix_auth_sessions_user ON auth_sessions (user_id);

-- One row per rotation generation. Only the SHA-256 hex of the token is stored;
-- the plaintext exists solely in the HttpOnly cookie.
CREATE TABLE refresh_tokens (
    id         uuid        PRIMARY KEY,
    session_id uuid        NOT NULL REFERENCES auth_sessions (id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    used_at    timestamptz,
    revoked_at timestamptz,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_session ON refresh_tokens (session_id);
