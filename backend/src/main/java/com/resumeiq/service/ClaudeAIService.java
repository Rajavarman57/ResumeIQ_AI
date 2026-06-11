package com.resumeiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class ClaudeAIService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    @Value("${anthropic.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Send a prompt to Claude and get back the text response.
     */
    public String ask(String systemPrompt, String userPrompt) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 2048);

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                body.put("system", systemPrompt);
            }

            ArrayNode messages = mapper.createArrayNode();
            ObjectNode msg = mapper.createObjectNode();
            msg.put("role", "user");
            msg.put("content", userPrompt);
            messages.add(msg);
            body.set("messages", messages);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude API error {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = mapper.readTree(response.body());
            return root.path("content").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Ask Claude to return JSON only. Strips markdown fences if present.
     */
    public JsonNode askJson(String systemPrompt, String userPrompt) {
        String raw = ask(systemPrompt, userPrompt);
        if (raw == null) return null;
        // Strip ```json ... ``` fences
        raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            log.error("Failed to parse Claude JSON response: {}", raw);
            return null;
        }
    }
}
