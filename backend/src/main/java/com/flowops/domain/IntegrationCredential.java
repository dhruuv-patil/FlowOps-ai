package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * The AES-256-GCM encrypted secret for an {@link Integration} (1:1).
 *
 * <p>{@code ciphertext} is an opaque {@code base64(iv ‖ ct ‖ tag)} blob produced by
 * {@code CredentialCipher}. It is written by the service layer on connect and read
 * back only at the execution-engine boundary, where it is decrypted in memory to
 * deliver a message. It is never returned to the client and never logged. Deleted
 * (cascade) when the integration is disconnected.
 */
@Entity
@Table(name = "integration_credentials")
public class IntegrationCredential {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "integration_id", nullable = false, updatable = false)
    private UUID integrationId;

    @Column(name = "ciphertext", nullable = false)
    private String ciphertext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationCredential() {
        // JPA
    }

    public static IntegrationCredential create(UUID integrationId, String ciphertext) {
        IntegrationCredential credential = new IntegrationCredential();
        credential.id = UUID.randomUUID();
        credential.integrationId = integrationId;
        credential.ciphertext = ciphertext;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        credential.createdAt = now;
        credential.updatedAt = now;
        return credential;
    }

    /** Replaces the encrypted secret (used when reconnecting an existing integration). */
    public void updateCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIntegrationId() {
        return integrationId;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
