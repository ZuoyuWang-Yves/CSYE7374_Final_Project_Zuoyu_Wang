package org.example.service;

import org.example.model.EscalationResult;
import org.example.model.EscalationTarget;
import org.example.model.FinalDecision;
import org.example.model.Severity;

/**
 * Holds the routing rules used by EscalationActor.
 */
public class EscalationRules {

    public EscalationResult route(FinalDecision finalDecision) {
        EscalationTarget target = toTarget(finalDecision.finalSeverity());
        String message = "Route " + finalDecision.eventId() + " to " + target.displayName() + ".";

        return new EscalationResult(finalDecision.eventId(), target, message);
    }

    private EscalationTarget toTarget(Severity severity) {
        return switch (severity) {
            case LOW -> EscalationTarget.NURSE;
            case MEDIUM -> EscalationTarget.GENERAL_DOCTOR;
            case HIGH -> EscalationTarget.SPECIALIST;
            case CRITICAL -> EscalationTarget.SENIOR_DOCTOR;
        };
    }
}
