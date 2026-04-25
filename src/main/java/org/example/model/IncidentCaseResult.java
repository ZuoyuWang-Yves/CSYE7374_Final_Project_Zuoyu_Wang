package org.example.model;

/**
 * Stores the full end-to-end result for one incident case.
 */
public record IncidentCaseResult(
        NormalizedIncident incident,
        RiskResult riskResult,
        HistoryResult historyResult,
        PolicyResult policyResult,
        LlmAnalysis llmAnalysis,
        FinalDecision finalDecision,
        EscalationResult escalationResult,
        AuditEvent auditEvent
) {
}
