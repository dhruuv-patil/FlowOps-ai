package com.flowops.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@code {{ path }}} template syntax used throughout node config
 * against a run's variable root.
 *
 * <p>The root is an object whose top-level keys are {@code trigger} (the trigger
 * payload) and one entry per already-succeeded node (keyed by node id, and by node
 * type as a convenience), each holding that node's output. A path like
 * {@code http_request.body.items.0.id} walks objects by key and arrays by numeric
 * index. An unresolved path yields {@link MissingNode}, which renders as the empty
 * string when interpolated — a missing variable never blows up a run.
 */
@Component
public class VariableInterpolator {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    /**
     * Replaces every {@code {{ path }}} in {@code template} with the resolved value.
     * Scalars use their text form; objects and arrays are inlined as compact JSON so
     * a template can build a JSON body from an upstream object.
     */
    public String interpolate(String template, JsonNode root) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            JsonNode value = resolve(root, matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(render(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Resolves a dotted path to a node, or {@link MissingNode} if any segment is absent. */
    public JsonNode resolve(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return MissingNode.getInstance();
        }
        JsonNode current = root;
        for (String rawSegment : path.trim().split("\\.")) {
            String segment = rawSegment.trim();
            if (segment.isEmpty() || current == null || current.isMissingNode() || current.isNull()) {
                return MissingNode.getInstance();
            }
            if (current.isArray()) {
                Integer index = asIndex(segment);
                current = index == null ? MissingNode.getInstance() : current.path(index);
            } else {
                current = current.path(segment);
            }
        }
        return current == null ? MissingNode.getInstance() : current;
    }

    private String render(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }

    private Integer asIndex(String segment) {
        try {
            return Integer.valueOf(segment);
        } catch (NumberFormatException notAnIndex) {
            return null;
        }
    }
}
