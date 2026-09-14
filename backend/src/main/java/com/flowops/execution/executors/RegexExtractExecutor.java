package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Regex Extract: extracts a capture group from text by pattern. */
@Component
public class RegexExtractExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "regex_extract";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        String patternStr = ctx.configString("pattern");
        String flagsStr = ctx.configString("flags");
        String groupStr = ctx.configString("group");

        if (input == null || input.isBlank()) {
            return NodeResult.fail("Regex Extract requires an 'input' string.");
        }
        if (patternStr == null || patternStr.isBlank()) {
            return NodeResult.fail("Regex Extract requires a 'pattern'.");
        }

        String interpolatedInput = ctx.interpolate(input);
        String interpolatedPattern = ctx.interpolate(patternStr);

        int flags = 0;
        if (flagsStr != null) {
            if ("CASE_INSENSITIVE".equalsIgnoreCase(flagsStr)) flags = Pattern.CASE_INSENSITIVE;
            else if ("MULTILINE".equalsIgnoreCase(flagsStr)) flags = Pattern.MULTILINE;
            else if ("NONE".equalsIgnoreCase(flagsStr)) flags = 0;
        }

        int group = 0;
        if (groupStr != null && !groupStr.isBlank()) {
            try {
                group = Integer.parseInt(groupStr);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            Pattern pattern = Pattern.compile(interpolatedPattern, flags);
            Matcher matcher = pattern.matcher(interpolatedInput);
            String result = "";
            if (matcher.find()) {
                if (group >= 0 && group <= matcher.groupCount()) {
                    result = matcher.group(group) == null ? "" : matcher.group(group);
                }
            }

            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("match", result);
            output.put("found", !result.isEmpty());
            ctx.log().info("Regex Extract: " + (result.isEmpty() ? "no match" : "matched") + ".");
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.fail("Regex Extract failed: " + e.getMessage());
        }
    }
}