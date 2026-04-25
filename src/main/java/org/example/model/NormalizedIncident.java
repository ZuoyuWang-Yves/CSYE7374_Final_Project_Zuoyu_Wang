package org.example.model;

import java.time.Instant;

/**
 * Stores the cleaned and validated incident data used in the pipeline.
 */
public record NormalizedIncident(
        String eventId,
        String system,
        String eventType,
        Severity severity,
        String details,
        Instant timestamp
) {
}
