package org.example.service;

import org.example.model.IncidentCaseResult;
import org.example.model.RawIncident;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IncidentRegistry {

    private final Map<String, RawIncident> submittedIncidentsByEventId = new LinkedHashMap<>();
    private final Map<String, IncidentCaseResult> completedResultsByEventId = new LinkedHashMap<>();

    public synchronized void recordSubmission(RawIncident rawIncident) {
        submittedIncidentsByEventId.put(rawIncident.eventId(), rawIncident);
    }

    public synchronized void recordCompletedResult(IncidentCaseResult result) {
        completedResultsByEventId.put(result.incident().eventId(), result);
    }

    public synchronized RawIncident findSubmittedIncident(String eventId) {
        return submittedIncidentsByEventId.get(eventId);
    }

    public synchronized IncidentCaseResult findCompletedResult(String eventId) {
        return completedResultsByEventId.get(eventId);
    }

    public synchronized List<IncidentCaseResult> findAllCompletedResults() {
        List<IncidentCaseResult> results = new ArrayList<>(completedResultsByEventId.values());
        results.sort(Comparator.comparing(
                (IncidentCaseResult result) -> result.auditEvent().timestamp()
        ).reversed());
        return results;
    }

    public synchronized List<String> findAllKnownEventIds() {
        List<String> eventIds = new ArrayList<>();

        for (IncidentCaseResult result : findAllCompletedResults()) {
            eventIds.add(result.incident().eventId());
        }

        List<String> submittedOnlyIds = new ArrayList<>(submittedIncidentsByEventId.keySet());
        submittedOnlyIds.removeAll(eventIds);
        java.util.Collections.reverse(submittedOnlyIds);
        eventIds.addAll(submittedOnlyIds);

        return eventIds;
    }
}
