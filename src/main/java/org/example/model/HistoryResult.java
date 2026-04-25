package org.example.model;

public record HistoryResult(
        String eventId,
        int similarIncidentCount,
        boolean repeatedFailure,
        String summary
) {
}
