package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiLlmProvider implements LlmProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final OpenAiResponsesClient openAiClient;

    public OpenAiLlmProvider(OpenAiResponsesClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    @Override
    public LlmAnalysis analyze(IncidentContext context) {
        try {
            String jsonResponse = openAiClient.createStructuredJsonResponse(
                    """
                            You are assisting a simulated medical software incident triage system.
                            Use the provided deterministic context carefully.
                            Return only the requested JSON object.
                            Do not invent unsupported facts.
                            """.strip(),
                    buildPrompt(context),
                    "triage_llm_analysis",
                    buildSchema(),
                    300
            );

            JsonNode responseNode = OBJECT_MAPPER.readTree(jsonResponse);

            return new LlmAnalysis(
                    context.incident().eventId(),
                    responseNode.path("analysis").asText(),
                    responseNode.path("recommendedAction").asText(),
                    responseNode.path("confidence").asDouble(0.0),
                    responseNode.path("valid").asBoolean(false)
            );
        } catch (Exception exception) {
            return new LlmAnalysis(
                    context.incident().eventId(),
                    "OpenAI request failed: " + exception.getMessage(),
                    "Use deterministic fallback behavior.",
                    0.0,
                    false
            );
        }
    }

    private String buildPrompt(IncidentContext context) throws Exception {
        Map<String, Object> promptContext = new LinkedHashMap<>();
        promptContext.put("incident", context.incident());
        promptContext.put("risk", context.risk());
        promptContext.put("history", context.history());
        promptContext.put("policy", context.policy());

        return """
                Analyze this simulated medical software incident context.
                Produce:
                - analysis: concise explanation
                - recommendedAction: concise recommendation
                - confidence: number from 0.0 to 1.0
                - valid: true when analysis completed normally

                Incident context:
                %s
                """.formatted(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(promptContext));
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", java.util.List.of("analysis", "recommendedAction", "confidence", "valid"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("analysis", Map.of("type", "string"));
        properties.put("recommendedAction", Map.of("type", "string"));
        properties.put("confidence", Map.of("type", "number"));
        properties.put("valid", Map.of("type", "boolean"));
        schema.put("properties", properties);

        return schema;
    }
}
