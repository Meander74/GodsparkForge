package com.godspark.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AiClient {

    private final HttpClient httpClient;
    private final AiConfig config;

    public AiClient(AiConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.timeoutMs()))
            .build();
    }

    public CompletableFuture<String> complete(List<ChatMessage> messages) {
        String body = buildRequestBody(messages);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.endpoint()))
            .timeout(Duration.ofMillis(config.timeoutMs()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException(
                        "AI endpoint returned " + response.statusCode() + ": " + truncate(response.body(), 200)
                    );
                }
                return parseResponseContent(response.body());
            });
    }

    private String buildRequestBody(List<ChatMessage> messages) {
        JsonObject root = new JsonObject();
        root.addProperty("max_tokens", config.maxTokens());
        root.addProperty("temperature", config.temperature());

        if (!config.model().isBlank()) {
            root.addProperty("model", config.model());
        }

        JsonArray messagesArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role", msg.role());
            obj.addProperty("content", msg.content());
            messagesArray.add(obj);
        }
        root.add("messages", messagesArray);

        return root.toString();
    }

    private static String parseResponseContent(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("AI response has no choices");
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            if (message == null) {
                throw new RuntimeException("AI response has no message");
            }
            JsonElement contentElement = message.get("content");
            if (contentElement == null || contentElement.isJsonNull()) {
                throw new RuntimeException("AI response message has no content");
            }
            String content = contentElement.getAsString();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("AI response content is empty");
            }
            return content.trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    public record ChatMessage(String role, String content) {}
}
