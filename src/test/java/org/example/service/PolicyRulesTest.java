package org.example.service;

import org.example.model.HistoryResult;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyRulesTest {

    private final PolicyRules policyRules = new PolicyRules();

    @Test
    void criticalRiskRequiresHumanReviewAndDisallowsAutoDismiss() {
        RiskResult risk = new RiskResult(
                "EVT-1001",
                Severity.CRITICAL,
                "Critical risk"
        );
        HistoryResult history = new HistoryResult(
                "EVT-1001",
                1,
                false,
                "No repeated failure"
        );

        PolicyResult result = policyRules.evaluate(risk, history);

        assertTrue(result.humanReviewRequired());
        assertFalse(result.autoDismissAllowed());
        assertTrue(result.appliedPolicies().contains("CRITICAL_REQUIRES_HUMAN_REVIEW"));
    }

    @Test
    void lowRiskWithoutRepeatedFailureUsesStandardMonitoring() {
        RiskResult risk = new RiskResult(
                "EVT-1001",
                Severity.LOW,
                "Low risk"
        );
        HistoryResult history = new HistoryResult(
                "EVT-1001",
                1,
                false,
                "No repeated failure"
        );

        PolicyResult result = policyRules.evaluate(risk, history);

        assertTrue(result.autoDismissAllowed());
        assertTrue(result.appliedPolicies().contains("STANDARD_MONITORING"));
    }
}
