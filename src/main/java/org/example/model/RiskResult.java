package org.example.model;

public record RiskResult(
        String eventId,
        Severity riskLevel,
        String reason
) {
}
