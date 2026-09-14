package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Merge: combines objects or concatenates arrays from multiple sources. */
@Component
public class MergeExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "merge";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        Object rawSources = ctx.config().get("sources");
        JsonNode sourcesNode = rawSources instanceof JsonNode ? (JsonNode) rawSources : null;
        String mode = ctx.configString("mode");
        if (sourcesNode == null || !sourcesNode.isArray()) {
            return NodeResult.fail("Merge requires a 'sources' array of paths.");
        }

        boolean objectMode = !"array".equals(mode);

        if (objectMode) {
            ObjectNode merged = ctx.mapper().createObjectNode();
            for (JsonNode src : sourcesNode) {
                String path = src.asText();
                JsonNode val = ctx.resolve(path);
                if (!val.isMissingNode() && val.isObject()) {
                    val.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue()));
                }
            }
            ctx.log().info("Merge combined " + sourcesNode.size() + " objects.");
            return NodeResult.success(merged);
        } else {
            ArrayNode merged = ctx.mapper().createArrayNode();
            for (JsonNode src : sourcesNode) {
                String path = src.asText();
                JsonNode val = ctx.resolve(path);
                if (!val.isMissingNode() && val.isArray()) {
                    for (JsonNode item : val) {
                        merged.add(item);
                    }
                }
            }
            ctx.log().info("Merge concatenated arrays into " + merged.size() + " items.");
            return NodeResult.success(merged);
        }
    }
}