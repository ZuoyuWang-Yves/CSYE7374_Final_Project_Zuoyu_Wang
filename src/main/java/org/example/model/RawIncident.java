package org.example.model;

public record RawIncident(
        String eventId,
        String system,
        String eventType,
        String severity,
        String details
) {
}
