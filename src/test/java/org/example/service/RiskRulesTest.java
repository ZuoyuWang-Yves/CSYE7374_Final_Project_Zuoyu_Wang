package org.example.service;

import org.example.model.NormalizedIncident;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskRulesTest {

    private final RiskRules riskRules = new RiskRules();

    @Test
    void oxygenDropHighBecomesCritical() {
        NormalizedIncident incident = incident("oxygen_drop", Severity.HIGH);

        RiskResult result = riskRules.analyze(incident);

        assertEquals(Severity.CRITICAL, result.riskLevel());
    }

    @Test
    void unknownEventKeepsInputSeverity() {
        NormalizedIncident incident = incident("battery_warning", Severity.MEDIUM);

        RiskResult result = riskRules.analyze(incident);

        assertEquals(Severity.MEDIUM, result.riskLevel());
    }

    private NormalizedIncident incident(String eventType, Severity severity) {
        return new NormalizedIncident(
                "EVT-1001",
                "TestSystem",
                eventType,
                severity,
                "Test details",
                Instant.now()
        );
    }
}
