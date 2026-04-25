package org.example.service;

import org.example.model.FinalAction;
import org.example.model.FinalDecision;
import org.example.model.LlmAnalysis;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;

/**
 * Small service that builds the final decision from the rule results.
 */
public class DecisionService {

    public FinalDecision decide(RiskResult riskResult, PolicyResult policyResult, LlmAnalysis llmAnalysis) {
        boolean llmAccepted = llmAnalysis != null && llmAnalysis.valid();
        Severity finalSeverity = riskResult.riskLevel();

        FinalAction finalAction = actionFor(finalSeverity, policyResult.humanReviewRequired());
        String reason = decisionReason(riskResult, llmAccepted, llmAnalysis);

        return new FinalDecision(
                riskResult.eventId(),
                finalSeverity,
                finalAction,
                policyResult.humanReviewRequired(),
                llmAccepted,
                reason,
                policyResult.appliedPolicies()
        );
    }

    private FinalAction actionFor(Severity severity, boolean humanReviewRequired) {
        if (severity == Severity.CRITICAL || severity == Severity.HIGH) {
            return FinalAction.ESCALATE;
        }

        if (humanReviewRequired || severity == Severity.MEDIUM) {
            return FinalAction.REVIEW;
        }

        return FinalAction.MONITOR;
    }

    private String decisionReason(RiskResult riskResult, boolean llmAccepted, LlmAnalysis llmAnalysis) {
        if (!llmAccepted) {
            return riskResult.reason() + " LLM output was unavailable or invalid, so deterministic fallback was used.";
        }

        return riskResult.reason() + " LLM recommendation accepted: " + llmAnalysis.recommendedAction();
    }
}
