package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Deduplicate: skips duplicate payload by key; first occurrence passes. */
@Component
public class DeduplicateExecutor implements NodeExecutor {

    // In-memory per-process; for true durability across restarts, use a database
    private static final ConcurrentHashMap<String, Boolean> seenKeys = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "deduplicate";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String key = ctx.configString("key");
        if (key == null || key.isBlank()) {
            return NodeResult.fail("Deduplicate requires a 'key'.");
        }

        String interpolatedKey = ctx.interpolate(key);
        boolean isDuplicate = seenKeys.putIfAbsent(interpolatedKey, true) != null;
        String handle = isDuplicate ? "duplicate" : "passed";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("key", interpolatedKey);
        output.put("duplicate", isDuplicate);

        ctx.log().info("Deduplicate: key=" + interpolatedKey + " → " + handle + ".");
        return NodeResult.success(output, java.util.List.of(handle));
    }
}