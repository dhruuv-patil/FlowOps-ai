package com.flowops.repository;

import com.flowops.domain.ExecutionLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, UUID> {

    List<ExecutionLog> findByExecutionIdOrderBySeqAsc(UUID executionId);

    /** Used to seed the per-run seq counter when resuming an execution. */
    int countByExecutionId(UUID executionId);
}
