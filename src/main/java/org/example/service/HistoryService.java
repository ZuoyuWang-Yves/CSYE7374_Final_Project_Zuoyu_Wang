package org.example.service;

import org.example.model.HistoryResult;
import org.example.model.NormalizedIncident;

import java.util.HashMap;
import java.util.Map;

public class HistoryService {

    private final Map<String, Integer> incidentCountsByType = new HashMap<>();

    public HistoryResult analyze(NormalizedIncident incident) {
        int previousCount = incidentCountsByType.getOrDefault(incident.eventType(), 0); // use getOrDefault to avoid null value if key not exists
        int updatedCount = previousCount + 1;
        incidentCountsByType.put(incident.eventType(), updatedCount);

        boolean repeatedFailure = updatedCount >= 3;
        String summary = repeatedFailure
                ? "Repeated failure detected for event type " + incident.eventType() + "."
                : "No repeated failure pattern detected.";

        return new HistoryResult(
                incident.eventId(),
                updatedCount,
                repeatedFailure,
                summary
        );
    }
}
