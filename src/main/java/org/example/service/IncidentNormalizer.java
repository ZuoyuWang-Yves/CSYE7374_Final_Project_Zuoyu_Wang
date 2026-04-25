package org.example.service;

import org.example.model.NormalizedIncident;
import org.example.model.RawIncident;
import org.example.model.Severity;

import java.time.Instant;
import java.util.UUID;

/**
 * Cleans raw incident input and turns it into a normalized incident.
 */
public class IncidentNormalizer {

    public NormalizedIncident normalize(RawIncident rawIncident) {
        if (rawIncident == null) {
            throw new IllegalArgumentException("Raw incident is required");
        }

        String eventId = normalizeEventId(rawIncident.eventId());
        String system = requireText(rawIncident.system(), "System is required");
        String eventType = requireText(rawIncident.eventType(), "Event type is required").toLowerCase();
        Severity severity = Severity.fromInput(rawIncident.severity());
        String details = requireText(rawIncident.details(), "Details are required");

        return new NormalizedIncident(
                eventId,
                system,
                eventType,
                severity,
                details,
                Instant.now()
        );
    }

    private String normalizeEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return eventId.trim().toUpperCase();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
