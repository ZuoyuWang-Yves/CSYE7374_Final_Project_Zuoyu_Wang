package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small client for calling the OpenAI Responses API.
 */
public class OpenAiResponsesClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String apiKey;
    private final String model;

    public OpenAiResponsesClient(String apiKey, String model, String endpoint) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.endpoint = URI.create(endpoint);
        this.apiKey = apiKey;
        this.model = model;
    }

    public String createTextResponse(String instructions, String input, int maxOutputTokens)
            throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("instructions", instructions);
        requestBody.put("input", input);
        requestBody.put("max_output_tokens", maxOutputTokens);

        String responseBody = sendRequest(requestBody);
        return extractOutputText(responseBody);
    }

    public String createStructuredJsonResponse(
            String instructions,
            String input,
            String schemaName,
            Map<String, Object> schema,
            int maxOutputTokens
    ) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("instructions", instructions);
        requestBody.put("input", input);
        requestBody.put("max_output_tokens", maxOutputTokens);
        requestBody.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", schemaName,
                        "strict", true,
                        "schema", schema
                )
        ));

        String responseBody = sendRequest(requestBody);
        return extractOutputText(responseBody);
    }

    private String sendRequest(Map<String, Object> requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI request failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode responseNode = OBJECT_MAPPER.readTree(responseBody);

        if (responseNode.hasNonNull("output_text")) {
            return responseNode.path("output_text").asText();
        }

        for (JsonNode outputItem : responseNode.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                if (contentItem.hasNonNull("text")) {
                    return contentItem.path("text").asText();
                }
            }
        }

        throw new IllegalStateException("OpenAI response did not contain generated text.");
    }
}
