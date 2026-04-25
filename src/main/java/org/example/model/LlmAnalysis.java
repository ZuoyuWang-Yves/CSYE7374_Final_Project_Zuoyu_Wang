package org.example.model;

/**
 * Stores the analysis returned by the LLM layer.
 */
public record LlmAnalysis(
        String eventId,
        String analysis,
        String recommendedAction,
        double confidence,
        boolean valid
) {
}
