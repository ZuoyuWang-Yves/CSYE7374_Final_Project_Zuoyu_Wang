package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.HistoryResult;
import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;
import org.example.model.NormalizedIncident;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.example.service.MockLlmProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LLMAnalysisActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void llmAnalysisActorRepliesWithStructuredAnalysis() {
        ActorRef<LLMAnalysisActor.Command> llmActor =
                testKit.spawn(LLMAnalysisActor.create(new MockLlmProvider()));
        TestProbe<LlmAnalysis> probe = testKit.createTestProbe(LlmAnalysis.class);

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

        llmActor.tell(new LLMAnalysisActor.AnalyzeIncidentContext(context, probe.getRef()));

        LlmAnalysis result = probe.receiveMessage();

        assertEquals("EVT-1001", result.eventId());
        assertTrue(result.valid());
        assertTrue(result.recommendedAction().contains("Escalate"));
    }
}
