package org.example.service;

import org.example.model.FinalAction;
import org.example.model.FinalDecision;
import org.example.model.LlmAnalysis;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionServiceTest {

    private final DecisionService decisionService = new DecisionService();

    @Test
    void criticalRiskEscalatesEvenWhenLlmIsValid() {
        RiskResult risk = new RiskResult("EVT-1001", Severity.CRITICAL, "Critical risk");
        PolicyResult policy = new PolicyResult(
                "EVT-1001",
                true,
                false,
                List.of("CRITICAL_REQUIRES_HUMAN_REVIEW")
        );
        LlmAnalysis llm = new LlmAnalysis(
                "EVT-1001",
                "Critical analysis",
                "Escalate immediately",
                0.95,
                true
        );

        FinalDecision decision = decisionService.decide(risk, policy, llm);

        assertEquals(Severity.CRITICAL, decision.finalSeverity());
        assertEquals(FinalAction.ESCALATE, decision.finalAction());
        assertTrue(decision.llmAccepted());
    }

    @Test
    void invalidLlmUsesDeterministicFallback() {
        RiskResult risk = new RiskResult("EVT-1001", Severity.HIGH, "High risk");
        PolicyResult policy = new PolicyResult(
                "EVT-1001",
                false,
                false,
                List.of("NO_AUTO_DISMISS_HIGH_RISK")
        );
        LlmAnalysis invalidLlm = new LlmAnalysis(
                "EVT-1001",
                "",
                "",
                0.0,
                false
        );

        FinalDecision decision = decisionService.decide(risk, policy, invalidLlm);

        assertEquals(FinalAction.ESCALATE, decision.finalAction());
        assertFalse(decision.llmAccepted());
    }
}
