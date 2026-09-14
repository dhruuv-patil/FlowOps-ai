package com.flowops.repository;

import com.flowops.domain.AiAgent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * All reads are org-scoped by construction: there is no bare {@code findById}
 * exposed to callers, so an agent can only ever be fetched together with its
 * owning organization id (contract §2 rule 6). This is what makes the
 * execution-engine's saved-agent lookup tenant-safe.
 */
public interface AiAgentRepository extends JpaRepository<AiAgent, UUID> {

    Optional<AiAgent> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<AiAgent> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
}
