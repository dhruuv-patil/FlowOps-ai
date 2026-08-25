package com.flowops.repository;

import com.flowops.domain.WorkflowVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberDesc(UUID workflowId);

    Optional<WorkflowVersion> findByWorkflowIdAndVersionNumber(UUID workflowId, int versionNumber);
}
