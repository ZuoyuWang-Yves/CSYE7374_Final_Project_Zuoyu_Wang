package org.example.model;

public record IncidentContext(
        NormalizedIncident incident,
        RiskResult risk,
        HistoryResult history,
        PolicyResult policy
) {
}
