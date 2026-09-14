package com.flowops.repository;

import com.flowops.domain.IntegrationWorkflow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for integration workflow mappings.
 * All reads are org-scoped by construction.
 */
public interface IntegrationWorkflowRepository extends JpaRepository<IntegrationWorkflow, UUID> {

    /** Find by workflow ID and organization (tenant isolation). */
    Optional<IntegrationWorkflow> findByIdAndOrganizationId(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);

    /** Find all monitored workflows for an integration. */
    @Query("SELECT w FROM IntegrationWorkflow w WHERE w.integrationId = :integrationId AND w.monitoringEnabled = true")
    List<IntegrationWorkflow> findByIntegrationIdAndMonitoringEnabledTrue(
            @Param("integrationId") UUID integrationId);

    /** True when the integration has at least one monitored workflow (scheduler's scan). */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM IntegrationWorkflow w "
            + "WHERE w.integrationId = :integrationId AND w.monitoringEnabled = true")
    boolean existsByIntegrationIdAndMonitoringEnabledTrue(
            @Param("integrationId") UUID integrationId);

    /** Check if a provider workflow is already mapped. */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM IntegrationWorkflow w "
            + "WHERE w.integrationId = :integrationId AND w.providerWorkflowId = :providerWorkflowId")
    boolean existsByIntegrationIdAndProviderWorkflowId(
            @Param("integrationId") UUID integrationId,
            @Param("providerWorkflowId") String providerWorkflowId);

    /** Find by integration and provider workflow id. */
    @Query("SELECT w FROM IntegrationWorkflow w WHERE w.integrationId = :integrationId AND w.providerWorkflowId = :providerWorkflowId")
    Optional<IntegrationWorkflow> findByIntegrationIdAndProviderWorkflowId(
            @Param("integrationId") UUID integrationId,
            @Param("providerWorkflowId") String providerWorkflowId);

    /** List workflows for an integration, newest first. */
    @Query("SELECT w FROM IntegrationWorkflow w WHERE w.integrationId = :integrationId ORDER BY w.updatedAt DESC")
    List<IntegrationWorkflow> findByIntegrationIdOrderByUpdatedAtDesc(
            @Param("integrationId") UUID integrationId);

    /** List workflows for an organization, newest first. */
    @Query("SELECT w FROM IntegrationWorkflow w WHERE w.organizationId = :organizationId ORDER BY w.updatedAt DESC")
    List<IntegrationWorkflow> findByOrganizationIdOrderByUpdatedAtDesc(
            @Param("organizationId") UUID organizationId,
            Pageable pageable);
}