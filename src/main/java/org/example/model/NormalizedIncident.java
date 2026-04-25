package org.example.model;

import java.time.Instant;

public record NormalizedIncident(
        String eventId,
        String system,
        String eventType,
        Severity severity,
        String details,
        Instant timestamp
) {
}
