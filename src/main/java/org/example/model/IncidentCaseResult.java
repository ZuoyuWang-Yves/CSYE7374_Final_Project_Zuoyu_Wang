package org.example.model;

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
