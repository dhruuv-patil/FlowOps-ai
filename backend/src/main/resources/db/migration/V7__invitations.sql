-- FlowOps M5 (slice 3) — team invitations.
--
-- Flyway owns the schema; spring.jpa.hibernate.ddl-auto is `validate`, so entity
-- mappings must match these columns exactly. UUIDs are generated in Java.
-- TIMESTAMPTZ always.
--
-- An `invitation` is a tokened, one-time link that lets an existing FlowOps user
-- join an organization at a chosen role. There is no email delivery in M5: an admin
-- generates the invite and hands the URL to the invitee out-of-band. The secret
-- token in the URL is the credential; only its SHA-256 hash (lowercase hex) is
-- stored here — NEVER the token itself — plus a non-secret last-4 `token_hint`. The
-- full URL is shown to the admin exactly once, at create time, and can never be
-- recovered from this row.
--
-- The invitation is bound to an email: accepting it requires the authenticated
-- user's email to match, so a leaked token cannot enroll an arbitrary account. Role
-- is constrained to ADMIN/MEMBER/VIEWER — an OWNER is only ever minted by creating
-- an organization, never by invitation.

CREATE TABLE invitations (
    id              uuid         PRIMARY KEY,
    organization_id uuid         NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    -- The invited address, normalized to lowercase. Matched against the accepting
    -- user's email; also used to render the pending-invite list.
    email           varchar(255) NOT NULL,
    role            varchar(16)  NOT NULL,
    -- Lowercase hex SHA-256 of the token. Exactly 64 chars. Never the token itself.
    token_hash      char(64)     NOT NULL,
    -- Non-secret display tail (last 4 chars of the token). Safe to render.
    token_hint      varchar(8),
    -- pending | accepted | revoked. A pending invite past expires_at is treated as
    -- not valid without a status change (checked at accept time).
    status          varchar(16)  NOT NULL,
    expires_at      timestamptz  NOT NULL,
    -- The admin who created it; nullable and SET NULL on user delete — the invitation
    -- belongs to the organization, not the person who minted its token.
    created_by      uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL,
    CONSTRAINT ck_invitations_role
        CHECK (role IN ('ADMIN', 'MEMBER', 'VIEWER')),
    CONSTRAINT ck_invitations_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED'))
);

-- At most one PENDING invite per (org, email): regenerating replaces the row's token.
-- A partial unique index lets accepted/revoked history coexist without collisions.
CREATE UNIQUE INDEX ux_invitations_org_email_pending
    ON invitations (organization_id, lower(email))
    WHERE status = 'PENDING';

-- Management listing is org-scoped.
CREATE INDEX ix_invitations_org ON invitations (organization_id);
-- Accept looks up by token hash (unique in practice; not enforced UNIQUE because a
-- revoked row keeps its hash and a fresh mint is astronomically unlikely to collide).
CREATE INDEX ix_invitations_token_hash ON invitations (token_hash);
