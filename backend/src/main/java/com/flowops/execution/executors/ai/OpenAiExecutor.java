package com.flowops.execution.executors.ai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowops.execution.NodeExecutionContext;
import com.flowops.execution.NodeExecutor;
import com.flowops.execution.NodeResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** OpenAI executor: runs LLM chat completion via OpenAI API. */
@Component
public class OpenAiExecutor implements NodeExecutor {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String type() {
        return "openai";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String prompt = ctx.configString("prompt");
        if (prompt == null || prompt.isBlank()) {
            return NodeResult.fail("OpenAI requires a 'prompt'.");
        }

        String apiKey = ctx.secret("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return NodeResult.fail("No connected OpenAI integration — configure an API Key.");
        }

        String model = ctx.configString("model");
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        String systemPrompt = ctx.configString("systemPrompt");
        String maxTokensStr = ctx.configString("maxTokens");
        int maxTokens = 500;
        if (maxTokensStr != null && !maxTokensStr.isBlank()) {
            try {
                maxTokens = Integer.parseInt(maxTokensStr.trim());
            } catch (NumberFormatException ignored) {
                // use default
            }
        }

        String interpolated = ctx.interpolate(prompt);
        String escapedSystem = escape(systemPrompt != null ? systemPrompt : "You are a helpful assistant.");
        String escapedPrompt = escape(interpolated);
        String escapedModel = escape(model);

        String payload = "{"
                + "\"model\":\"" + escapedModel + "\","
                + "\"max_tokens\":" + maxTokens + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapedSystem + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}"
                + "]}";

        try {
            URI uri = URI.create("https://api.openai.com/v1/chat/completions");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            ObjectNode output = ctx.mapper().createObjectNode();
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            output.put("status", response.statusCode());
            output.put("model", model);
            output.put("durationMs", duration);

            if (ok) {
                // Extract the content from the JSON response
                com.fasterxml.jackson.databind.JsonNode node = ctx.mapper().readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode contentNode = node.at("/choices/0/message/content");
                output.put("content", contentNode.isMissingNode() ? "" : contentNode.asText());
                ctx.log().info("OpenAI completion (" + model + "): " + duration + "ms");
                return NodeResult.success(output);
            } else {
                return NodeResult.fail("OpenAI API failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            return NodeResult.fail("OpenAI error: " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
