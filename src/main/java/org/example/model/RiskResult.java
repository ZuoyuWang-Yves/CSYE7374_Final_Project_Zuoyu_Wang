package org.example.model;

/**
 * Stores the risk check result for one incident.
 */
public record RiskResult(
        String eventId,
        Severity riskLevel,
        String reason
) {
}
