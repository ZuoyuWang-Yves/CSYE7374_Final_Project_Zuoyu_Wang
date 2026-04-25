package org.example.service;

import org.example.model.EscalationResult;
import org.example.model.EscalationTarget;
import org.example.model.FinalAction;
import org.example.model.FinalDecision;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscalationRulesTest {

    private final EscalationRules escalationRules = new EscalationRules();

    @Test
    void criticalDecisionRoutesToSeniorDoctor() {
        FinalDecision decision = new FinalDecision(
                "EVT-1001",
                Severity.CRITICAL,
                FinalAction.ESCALATE,
                true,
                true,
                "Critical incident",
                List.of("CRITICAL_REQUIRES_HUMAN_REVIEW")
        );

        EscalationResult result = escalationRules.route(decision);

        assertEquals(EscalationTarget.SENIOR_DOCTOR, result.target());
    }
}
