package org.example.service;

import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;
import org.example.model.Severity;

/**
 * Mock provider used for demos and tests.
 */
public class MockLlmProvider implements LlmProvider {

    @Override
    public LlmAnalysis analyze(IncidentContext context) {
        Severity riskLevel = context.risk().riskLevel();

        String analysis = buildAnalysis(context);
        String recommendedAction = recommendAction(riskLevel, context.policy().humanReviewRequired());
        double confidence = confidenceFor(riskLevel);

        return new LlmAnalysis(
                context.incident().eventId(),
                analysis,
                recommendedAction,
                confidence,
                true
        );
    }

    private String buildAnalysis(IncidentContext context) {
        String repeatedFailureText = context.history().repeatedFailure()
                ? " Recent history also shows repeated failures."
                : "";

        return "Incident " + context.incident().eventType()
                + " was analyzed with risk level "
                + context.risk().riskLevel()
                + "."
                + repeatedFailureText;
    }

    private String recommendAction(Severity riskLevel, boolean humanReviewRequired) {
        if (riskLevel == Severity.CRITICAL) {
            return "Escalate immediately to senior doctor.";
        }

        if (riskLevel == Severity.HIGH || humanReviewRequired) {
            return "Escalate to specialist and require human review.";
        }

        if (riskLevel == Severity.MEDIUM) {
            return "Route to general doctor for review.";
        }

        return "Continue monitoring.";
    }

    private double confidenceFor(Severity riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> 0.95;
            case HIGH -> 0.88;
            case MEDIUM -> 0.76;
            case LOW -> 0.67;
        };
    }
}
