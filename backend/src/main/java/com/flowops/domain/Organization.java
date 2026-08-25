package com.flowops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** A tenant. {@code name} is as typed and not unique; {@code slug} is derived and globally unique. */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 100, updatable = false)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organization() {
        // JPA
    }

    public static Organization create(String name, String slug) {
        Organization organization = new Organization();
        organization.id = UUID.randomUUID();
        organization.name = name;
        organization.slug = slug;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        organization.createdAt = now;
        organization.updatedAt = now;
        return organization;
    }

    /**
     * Renames the organization. The slug is immutable by design — existing links
     * keep working across a rename.
     */
    public void rename(String newName) {
        this.name = newName;
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
