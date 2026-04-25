package org.example.model;

/**
 * Stores the history check result for one incident.
 */
public record HistoryResult(
        String eventId,
        int similarIncidentCount,
        boolean repeatedFailure,
        String summary
) {
}
