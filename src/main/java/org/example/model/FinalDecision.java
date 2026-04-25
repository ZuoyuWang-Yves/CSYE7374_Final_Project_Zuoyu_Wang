package org.example.model;

import java.util.List;

public record FinalDecision(
        String eventId,
        Severity finalSeverity,
        FinalAction finalAction,
        boolean humanReviewRequired,
        boolean llmAccepted,
        String reason,
        List<String> policyApplied
) {
}
