package com.flowops.execution.executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import org.springframework.stereotype.Component;

/** Anomaly Detector: flags outliers in a numeric series using MAD. */
@Component
public class AnomalyDetectorExecutor implements NodeExecutor {

    @Override
    public String type() {
        return "anomaly_detector";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String seriesPath = ctx.configString("seriesPath");
        String zThresholdStr = ctx.configString("zThreshold");

        if (seriesPath == null || seriesPath.isBlank()) return NodeResult.fail("Anomaly Detector requires 'seriesPath'.");

        double zThreshold = 2.5;
        if (zThresholdStr != null && !zThresholdStr.isBlank()) {
            try { zThreshold = Double.parseDouble(zThresholdStr); } catch (NumberFormatException ignored) {}
        }

        JsonNode seriesNode = ctx.resolve(seriesPath);
        if (seriesNode.isMissingNode() || !seriesNode.isArray()) {
            return NodeResult.fail("Anomaly Detector: series path not found or not an array.");
        }

        // Extract numeric values
        double[] values = new double[seriesNode.size()];
        int count = 0;
        for (JsonNode n : seriesNode) {
            if (n.isNumber()) {
                values[count++] = n.asDouble();
            }
        }
        if (count < 2) {
            ObjectNode output = ctx.mapper().createObjectNode();
            output.put("anomalies", 0);
            output.put("insufficientData", true);
            return NodeResult.success(output);
        }

        // Compute median
        double[] sorted = java.util.Arrays.copyOf(values, count);
        java.util.Arrays.sort(sorted);
        double median = sorted[count / 2];

        // Compute MAD
        double[] deviations = new double[count];
        for (int i = 0; i < count; i++) {
            deviations[i] = Math.abs(values[i] - median);
        }
        java.util.Arrays.sort(deviations);
        double mad = deviations[count / 2];
        if (mad == 0) mad = 1e-9; // avoid division by zero

        // Flag anomalies
        ArrayNode anomalies = ctx.mapper().createArrayNode();
        for (int i = 0; i < count; i++) {
            double z = Math.abs(values[i] - median) / mad;
            if (z > zThreshold) {
                ObjectNode a = ctx.mapper().createObjectNode();
                a.put("index", i);
                a.put("value", values[i]);
                a.put("zScore", z);
                anomalies.add(a);
            }
        }

        ObjectNode output = ctx.mapper().createObjectNode();
        output.put("median", median);
        output.put("mad", mad);
        output.put("zThreshold", zThreshold);
        output.set("anomalies", anomalies);
        output.put("anomalyCount", anomalies.size());
        ctx.log().info("Anomaly Detector: " + anomalies.size() + " anomalies found.");
        return NodeResult.success(output);
    }
}