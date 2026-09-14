-- FlowOps M5 (slice 2, follow-up) — reconcile webhooks.token_hash with the entity.
--
-- V6 created token_hash as char(64). The JPA entity (Webhook.tokenHash: String,
-- @Column length = 64) maps to varchar(64), so under ddl-auto=validate Hibernate
-- rejected the char (Types.CHAR) column against its expected varchar (Types.VARCHAR).
-- Every other string column in the schema is varchar; char(64) was the lone outlier.
--
-- V6 is left untouched (Flyway history is immutable); this migration converts the
-- live column to varchar(64) to match the entity. The "exactly 64 lowercase-hex
-- chars" invariant that char(64) implied is preserved explicitly with a CHECK, which
-- Hibernate validate ignores. Idempotent-friendly: converting char(64) → varchar(64)
-- is a safe widening and preserves existing values.

ALTER TABLE webhooks
    ALTER COLUMN token_hash TYPE varchar(64);

ALTER TABLE webhooks
    ADD CONSTRAINT ck_webhooks_token_hash_len CHECK (char_length(token_hash) = 64);
