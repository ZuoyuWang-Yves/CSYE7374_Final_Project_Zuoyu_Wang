package org.example.service;

import org.example.model.HistoryResult;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;

import java.util.ArrayList;
import java.util.List;

public class PolicyRules {

    public PolicyResult evaluate(RiskResult riskResult, HistoryResult historyResult) {
        List<String> appliedPolicies = new ArrayList<>();

        boolean humanReviewRequired = false;
        boolean autoDismissAllowed = true;

        if (riskResult.riskLevel() == Severity.CRITICAL) {
            humanReviewRequired = true;
            autoDismissAllowed = false;
            appliedPolicies.add("CRITICAL_REQUIRES_HUMAN_REVIEW");
            appliedPolicies.add("NO_AUTO_DISMISS_CRITICAL");
        }

        if (riskResult.riskLevel() == Severity.HIGH) {
            autoDismissAllowed = false;
            appliedPolicies.add("NO_AUTO_DISMISS_HIGH_RISK");
        }

        if (historyResult.repeatedFailure()) {
            humanReviewRequired = true;
            appliedPolicies.add("REPEATED_FAILURE_REQUIRES_ESCALATION");
        }

        if (appliedPolicies.isEmpty()) {
            appliedPolicies.add("STANDARD_MONITORING");
        }

        return new PolicyResult(
                riskResult.eventId(),
                humanReviewRequired,
                autoDismissAllowed,
                List.copyOf(appliedPolicies) // Use copy for read-only(immutable)
        );
    }
}
