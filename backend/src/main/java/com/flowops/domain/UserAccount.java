package com.flowops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * A person. Mapped to {@code users}; the schema is owned by Flyway
 * ({@code V1__auth_and_orgs.sql}).
 *
 * <p>{@code passwordHash} is {@code @JsonIgnore}d and excluded from
 * {@link #toString()}. Response bodies are built field-by-field by
 * {@code UserResponse}, never by reflecting over this entity, so the hash has no
 * path to the wire, a log line, or an OpenAPI schema.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
        // JPA
    }

    /**
     * @param email        must already be normalized (trimmed, lowercased) by the
     *                     request record's compact constructor
     * @param passwordHash a BCrypt hash — never a plaintext password
     */
    public static UserAccount create(String email, String passwordHash, String fullName) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        // Belt and braces: the UNIQUE index is on lower(email).
        user.email = email.toLowerCase(Locale.ROOT);
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.avatarUrl = null;
        // Millisecond precision keeps the serialized ISO-8601 form stable across a
        // database round trip (Postgres stores microseconds).
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        // Deliberately omits passwordHash and email.
        return "UserAccount{id=" + id + "}";
    }
}
