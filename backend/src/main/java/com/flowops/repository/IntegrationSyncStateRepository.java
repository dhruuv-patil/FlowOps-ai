package com.flowops.repository;

import com.flowops.domain.IntegrationSyncState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for integration sync state.
 * All reads are org-scoped by construction.
 */
public interface IntegrationSyncStateRepository extends JpaRepository<IntegrationSyncState, UUID> {

    /** Find by integration ID and organization (tenant isolation). */
    Optional<IntegrationSyncState> findByOrganizationIdAndIntegrationId(
            @Param("organizationId") UUID organizationId,
            @Param("integrationId") UUID integrationId);
}