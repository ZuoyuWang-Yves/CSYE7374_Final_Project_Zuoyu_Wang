package org.example.model;

/**
 * Groups the incident data, risk, history, and policy into one object.
 */
public record IncidentContext(
        NormalizedIncident incident,
        RiskResult risk,
        HistoryResult history,
        PolicyResult policy
) {
}
