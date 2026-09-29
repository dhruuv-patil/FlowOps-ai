package com.flowops.execution.executors;

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

/** Telegram node executor: sends a message to Telegram. */
@Component
public class TelegramExecutor implements NodeExecutor {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String type() {
        return "telegram";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        String message = ctx.configString("message");
        if (message == null || message.isBlank()) {
            return NodeResult.fail("Telegram requires a 'message'.");
        }

        String botToken = ctx.secret("botToken");
        String chatId = ctx.configString("chatId");
        if (chatId == null || chatId.isBlank()) {
            chatId = ctx.secret("defaultChatId");
        }

        if (botToken == null || botToken.isBlank()) {
            return NodeResult.fail("No connected Telegram integration — configure credentials first.");
        }
        if (chatId == null || chatId.isBlank()) {
            return NodeResult.fail("Telegram requires a target 'chatId'.");
        }

        String interpolated = ctx.interpolate(message);
        try {
            URI uri = URI.create("https://api.telegram.org/bot" + botToken.trim() + "/sendMessage");
            String payload = "{\"chat_id\":\"" + escape(chatId) + "\",\"text\":\"" + escape(interpolated) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectNode output = ctx.mapper().createObjectNode();
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            output.put("delivered", ok);
            output.put("status", response.statusCode());
            output.put("chatId", chatId);
            output.put("message", interpolated);

            if (ok) {
                ctx.log().info("Telegram message delivered to " + chatId + ": " + response.statusCode());
                return NodeResult.success(output);
            } else {
                return NodeResult.fail("Telegram API failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            return NodeResult.fail("Telegram error: " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
