package org.example.model;

/**
 * Stores the final escalation target for one incident.
 */
public record EscalationResult(
        String eventId,
        EscalationTarget target,
        String message
) {
}
