-- FlowOps — reconcile invitations.token_hash with the entity (same fix as V9 did
-- for webhooks.token_hash).
--
-- V7 created token_hash as char(64). The JPA entity (Invitation.tokenHash: String,
-- @Column length = 64) maps to varchar(64), so under ddl-auto=validate Hibernate
-- rejected the char (Types.CHAR) column against its expected varchar (Types.VARCHAR),
-- which prevented the SessionFactory (and thus the whole ApplicationContext) from
-- starting. Every other string column in the schema is varchar; char(64) was the
-- outlier on token_hash columns.
--
-- V7 is left untouched (Flyway history is immutable); this converts the live column
-- to varchar(64) to match the entity. Data is preserved: token_hash holds exactly-64
-- lowercase-hex SHA-256 digests with no trailing padding, and Postgres trims trailing
-- blanks (of which there are none here) when casting bpchar -> varchar. The index
-- ix_invitations_token_hash is rebuilt automatically by the type change. The
-- "exactly 64 chars" invariant that char(64) implied is preserved explicitly with a
-- CHECK, which Hibernate validate ignores.

ALTER TABLE invitations
    ALTER COLUMN token_hash TYPE varchar(64);

ALTER TABLE invitations
    ADD CONSTRAINT ck_invitations_token_hash_len CHECK (char_length(token_hash) = 64);
