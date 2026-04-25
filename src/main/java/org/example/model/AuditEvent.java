package org.example.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Stores one audit log entry.
 */
public record AuditEvent(
        Instant timestamp,
        ZonedDateTime timestampEastern,
        String eventId,
        String actorName,
        String action,
        Map<String, Object> details
) {
}
