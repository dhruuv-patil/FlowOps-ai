package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Text Formatter: applies text operations (case, trim, replace, truncate, pad). */
@Component
public class TextFormatterExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "text_formatter";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        String operation = ctx.configString("operation");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("Text Formatter requires an 'input' string.");
        }
        if (operation == null || operation.isBlank()) {
            return NodeResult.fail("Text Formatter requires an 'operation'.");
        }

        String text = ctx.interpolate(input);
        String find = ctx.configString("find");
        String replaceWith = ctx.configString("replaceWith");
        String lengthStr = ctx.configString("length");
        String padChar = ctx.configString("padChar");

        String result;
        switch (operation) {
            case "upper" -> result = text.toUpperCase();
            case "lower" -> result = text.toLowerCase();
            case "title" -> result = toTitleCase(text);
            case "trim" -> result = text.trim();
            case "replace" -> {
                if (find == null) find = "";
                if (replaceWith == null) replaceWith = "";
                result = text.replace(find, replaceWith);
            }
            case "truncate" -> {
                int length = 100;
                try {
                    length = Integer.parseInt(lengthStr == null ? "100" : lengthStr);
                } catch (NumberFormatException ignored) {
                }
                if (text.length() > length) {
                    result = text.substring(0, length);
                } else {
                    result = text;
                }
            }
            case "pad" -> {
                int length = 100;
                try {
                    length = Integer.parseInt(lengthStr == null ? "100" : lengthStr);
                } catch (NumberFormatException ignored) {
                }
                String pad = padChar == null || padChar.isBlank() ? " " : padChar;
                if (text.length() < length) {
                    result = pad.repeat(length - text.length()) + text;
                } else {
                    result = text;
                }
            }
            default -> {
                return NodeResult.fail("Text Formatter unknown operation: " + operation);
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("result", result);
        ctx.log().info("Text Formatter: " + operation + " applied.");
        return NodeResult.success(output);
    }

    private String toTitleCase(String s) {
        StringBuilder sb = new StringBuilder();
        boolean nextTitle = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                nextTitle = true;
                sb.append(c);
            } else if (nextTitle) {
                sb.append(Character.toUpperCase(c));
                nextTitle = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}