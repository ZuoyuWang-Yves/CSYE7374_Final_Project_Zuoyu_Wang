package org.example.model;

/**
 * Stores the raw incident input before normalization.
 */
public record RawIncident(
        String eventId,
        String system,
        String eventType,
        String severity,
        String details
) {
}
