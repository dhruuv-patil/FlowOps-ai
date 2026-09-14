package com.flowops.repository;

import com.flowops.domain.IntegrationCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The encrypted-secret store. Credentials are only ever fetched by their owning
 * integration id — which the caller has already resolved within a tenant — so this
 * table is never queried directly by an untrusted key.
 */
public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, UUID> {

    Optional<IntegrationCredential> findByIntegrationId(UUID integrationId);

    void deleteByIntegrationId(UUID integrationId);
}
