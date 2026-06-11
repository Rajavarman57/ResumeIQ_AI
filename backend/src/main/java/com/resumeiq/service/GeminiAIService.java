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
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Send a prompt to Gemini and get back the text response.
     */
    public String ask(String systemPrompt, String userPrompt) {
        return ask(systemPrompt, userPrompt, false);
    }

    public String ask(String systemPrompt, String userPrompt, boolean forceJson) {
        try {
            ObjectNode body = mapper.createObjectNode();

            // 1. contents
            ArrayNode contents = mapper.createArrayNode();
            ObjectNode contentObj = mapper.createObjectNode();
            contentObj.put("role", "user");
            ArrayNode parts = mapper.createArrayNode();
            ObjectNode partText = mapper.createObjectNode();
            partText.put("text", userPrompt);
            parts.add(partText);
            contentObj.set("parts", parts);
            contents.add(contentObj);
            body.set("contents", contents);

            // 2. systemInstruction
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sysInst = mapper.createObjectNode();
                ArrayNode sysParts = mapper.createArrayNode();
                ObjectNode sysPartText = mapper.createObjectNode();
                sysPartText.put("text", systemPrompt);
                sysParts.add(sysPartText);
                sysInst.set("parts", sysParts);
                body.set("systemInstruction", sysInst);
            }

            // 3. generationConfig (JSON mode if requested)
            if (forceJson) {
                ObjectNode genConfig = mapper.createObjectNode();
                genConfig.put("responseMimeType", "application/json");
                body.set("generationConfig", genConfig);
            }

            // URL format: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
            String fullUrl = apiUrl + "/" + model + ":generateContent?key=" + apiKey;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
            return textNode.isMissingNode() ? null : textNode.asText();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Ask Gemini to return JSON only. Strips markdown fences if present.
     */
    public JsonNode askJson(String systemPrompt, String userPrompt) {
        String raw = ask(systemPrompt, userPrompt, true);
        if (raw == null) return null;
        // Strip ```json ... ``` fences if model still generates them (though JSON mode usually doesn't)
        raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response: {}", raw);
            return null;
        }
    }
}
