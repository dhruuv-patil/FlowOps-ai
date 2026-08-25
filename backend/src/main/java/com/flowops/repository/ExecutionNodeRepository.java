package com.flowops.repository;

import com.flowops.domain.ExecutionNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionNodeRepository extends JpaRepository<ExecutionNode, UUID> {

    /** All node rows for a run, in the order they were created (graph order). */
    List<ExecutionNode> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);

    Optional<ExecutionNode> findByExecutionIdAndNodeId(UUID executionId, String nodeId);
}
