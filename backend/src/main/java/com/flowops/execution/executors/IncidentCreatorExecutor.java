package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Incident Creator: creates an incident output and optionally records the
 * configured notification channel.
 *
 * <p>The incident is currently execution-scoped; persistent incident storage
 * should be added through the incident service/repository when that subsystem
 * exists.
 */
@Component
public class IncidentCreatorExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "incident_creator";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String title = ctx.configString("title");
        String severity = ctx.configString("severity");
        String body = ctx.configString("body");
        String channel = ctx.configString("channel");

        if (title == null || title.isBlank()) {
            return NodeResult.fail(
                    "Incident Creator requires 'title'.");
        }

        String interpolatedTitle = ctx.interpolate(title);
        String interpolatedSeverity =
                severity == null || severity.isBlank()
                        ? "MEDIUM"
                        : ctx.interpolate(severity);

        String interpolatedBody =
                body == null
                        ? ""
                        : ctx.interpolate(body);

        String interpolatedChannel =
                channel == null || channel.isBlank()
                        ? ""
                        : ctx.interpolate(channel);

        ObjectNode output = ctx.mapper().createObjectNode();

        output.put("incidentId", UUID.randomUUID().toString());
        output.put("title", interpolatedTitle);
        output.put("severity", interpolatedSeverity);
        output.put("body", interpolatedBody);
        output.put("createdAt", Instant.now().toString());

        if (!interpolatedChannel.isBlank()) {
            output.put("notificationChannel", interpolatedChannel);
            ctx.log().info(
                    "Incident created with notification via "
                            + interpolatedChannel
                            + ": "
                            + interpolatedTitle);
        } else {
            ctx.log().info(
                    "Incident created: " + interpolatedTitle);
        }

        return NodeResult.success(output);
    }
}