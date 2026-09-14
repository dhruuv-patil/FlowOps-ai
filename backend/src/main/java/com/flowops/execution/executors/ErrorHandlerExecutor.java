package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Error Handler: sanitizes and annotates upstream error, branches handled/risky. */
@Component
public class ErrorHandlerExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "error_handler";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String errorPath = ctx.configString("errorPath");
        String maxLengthStr = ctx.configString("maxLength");
        if (errorPath == null || errorPath.isBlank()) {
            return NodeResult.fail("Error Handler requires an 'errorPath'.");
        }

        int maxLength = 200;
        if (maxLengthStr != null && !maxLengthStr.isBlank()) {
            try {
                maxLength = Integer.parseInt(maxLengthStr);
            } catch (NumberFormatException ignored) {
            }
        }

        JsonNode errorNode = ctx.resolve(errorPath);
        String error = "";
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            error = errorNode.isTextual() ? errorNode.asText() : errorNode.toString();
        }

        // Sanitize: single line, bounded, no null
        String cleaned = error.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength) + "…";
        }
        if (cleaned.isBlank()) {
            cleaned = "Unknown error.";
        }

        boolean risky = cleaned.toLowerCase().contains("timeout")
                || cleaned.toLowerCase().contains("connection")
                || cleaned.toLowerCase().contains("500")
                || cleaned.toLowerCase().contains("502")
                || cleaned.toLowerCase().contains("503");
        String handle = risky ? "risky" : "handled";

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("originalError", error);
        output.put("sanitizedError", cleaned);
        output.put("risky", risky);

        ctx.log().info("Error Handler: " + handle + " — " + cleaned);
        return NodeResult.success(output, java.util.List.of(handle));
    }
}