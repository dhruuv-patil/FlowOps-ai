package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** CSV Parse: parses delimited text into rows of values. */
@Component
public class CsvParseExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "csv_parse";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String input = ctx.configString("input");
        if (input == null || input.isBlank()) {
            return NodeResult.fail("CSV Parse requires an 'input' string.");
        }

        String interpolated = ctx.interpolate(input);
        boolean hasHeader = Boolean.TRUE.equals(ctx.config().get("hasHeader"));
        String delimiter = ctx.configString("delimiter");
        if (delimiter == null || delimiter.isBlank()) {
            delimiter = ",";
        }

        String[] lines = interpolated.split("\\r?\\n");
        if (lines.length == 0) {
            return NodeResult.fail("CSV Parse: empty input.");
        }

        ArrayNode rows = ctx.mapper().createArrayNode();
        String[] headers = null;
        int start = 0;

        if (hasHeader && lines.length > 0) {
            headers = splitCsvLine(lines[0], delimiter);
            start = 1;
        }

        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] fields = splitCsvLine(line, delimiter);
            ObjectNode row = ctx.mapper().createObjectNode();
            for (int j = 0; j < fields.length; j++) {
                String key = (headers != null && j < headers.length) ? headers[j] : "col" + j;
                row.put(key, fields[j]);
            }
            rows.add(row);
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.set("rows", rows);
        output.put("count", rows.size());
        ctx.log().info("CSV Parse produced " + rows.size() + " rows.");
        return NodeResult.success(output);
    }

    private String[] splitCsvLine(String line, String delimiter) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter.charAt(0) && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}