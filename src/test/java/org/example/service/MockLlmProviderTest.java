package org.example.service;

import org.example.model.HistoryResult;
import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;
import org.example.model.NormalizedIncident;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockLlmProviderTest {

    private final MockLlmProvider mockLlmProvider = new MockLlmProvider();

    @Test
    void mockLlmReturnsValidAnalysis() {
        IncidentContext context = new IncidentContext(
                new NormalizedIncident(
                        "EVT-1001",
                        "VitalsMonitor",
                        "oxygen_drop",
                        Severity.HIGH,
                        "SpO2 < 88% for 40s",
                        Instant.now()
                ),
                new RiskResult("EVT-1001", Severity.CRITICAL, "Critical risk"),
                new HistoryResult("EVT-1001", 3, true, "Repeated failure"),
                new PolicyResult("EVT-1001", true, false, List.of("NO_AUTO_DISMISS_CRITICAL"))
        );

        LlmAnalysis analysis = mockLlmProvider.analyze(context);

        assertTrue(analysis.valid());
        assertFalse(analysis.analysis().isBlank());
        assertFalse(analysis.recommendedAction().isBlank());
    }
}
