package org.example.model;

public record LlmAnalysis(
        String eventId,
        String analysis,
        String recommendedAction,
        double confidence,
        boolean valid
) {
}
