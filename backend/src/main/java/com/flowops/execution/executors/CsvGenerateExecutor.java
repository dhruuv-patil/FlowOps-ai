package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** CSV Generate: serializes an array of objects into delimited text. */
@Component
public class CsvGenerateExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "csv_generate";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String rowsPath = ctx.configString("rows");
        if (rowsPath == null || rowsPath.isBlank()) {
            return NodeResult.fail("CSV Generate requires a 'rows' path.");
        }

        JsonNode rowsNode = ctx.resolve(rowsPath);
        if (rowsNode.isMissingNode() || !rowsNode.isArray()) {
            return NodeResult.fail("CSV Generate rows path '" + rowsPath + "' not found or not an array.");
        }

        Object rawColumns = ctx.config().get("columns");
        JsonNode columnsNode = rawColumns instanceof JsonNode ? (JsonNode) rawColumns : null;
        java.util.List<String> columns = new java.util.ArrayList<>();
        if (columnsNode != null && columnsNode.isArray()) {
            for (JsonNode col : columnsNode) {
                columns.add(col.asText());
            }
        }

        String delimiter = ctx.configString("delimiter");
        if (delimiter == null || delimiter.isBlank()) {
            delimiter = ",";
        }

        StringBuilder csv = new StringBuilder();

        // If columns specified, write header
        if (!columns.isEmpty()) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) csv.append(delimiter);
                csv.append(escape(columns.get(i)));
            }
            csv.append("\n");
        }

        for (JsonNode row : rowsNode) {
            if (!row.isObject()) continue;
            if (columns.isEmpty()) {
                // First row determines columns
                java.util.Iterator<String> it = row.fieldNames();
                while (it.hasNext()) {
                    columns.add(it.next());
                }
            }
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) csv.append(delimiter);
                JsonNode val = row.get(columns.get(i));
                String cell = val != null && !val.isNull() ? val.asText() : "";
                csv.append(escape(cell));
            }
            csv.append("\n");
        }

        String result = csv.toString();
        com.fasterxml.jackson.databind.node.ObjectNode output = ctx.mapper().createObjectNode();
        output.put("csv", result);
        output.put("rows", rowsNode.size());
        ctx.log().info("CSV Generate produced " + rowsNode.size() + " rows.");
        return NodeResult.success(output);
    }

    private String escape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}