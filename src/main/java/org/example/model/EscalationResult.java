package org.example.model;

public record EscalationResult(
        String eventId,
        EscalationTarget target,
        String message
) {
}
