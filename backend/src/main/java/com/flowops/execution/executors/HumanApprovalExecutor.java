package com.flowops.execution.executors;

import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/**
 * Human Approval: suspends the run and waits for a person to approve or reject.
 *
 * <p>The executor only ever returns {@link NodeResult#waiting()}; the engine parks
 * the execution in {@code WAITING} and releases the worker. When a decision arrives
 * via the resume endpoint, the run is re-queued with this node resolved to the
 * chosen {@code approved}/{@code rejected} port — so the executor is not re-run and
 * the human's decision, not a re-evaluation, drives the branch.
 */
@Component
public class HumanApprovalExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "human_approval";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String prompt = ctx.configString("prompt");
        ctx.log().info("Waiting for human approval"
                + (prompt == null || prompt.isBlank() ? "." : ": " + prompt));
        return NodeResult.waiting();
    }
}
