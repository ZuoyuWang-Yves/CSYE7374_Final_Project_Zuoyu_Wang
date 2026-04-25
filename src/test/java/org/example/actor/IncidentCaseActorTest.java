package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.IncidentCaseResult;
import org.example.model.NormalizedIncident;
import org.example.model.Severity;
import org.example.service.DecisionService;
import org.example.service.EscalationRules;
import org.example.service.HistoryService;
import org.example.service.MockLlmProvider;
import org.example.service.PolicyRules;
import org.example.service.RiskRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IncidentCaseActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void incidentCaseActorProcessesOneCriticalIncidentEndToEnd() throws IOException {
        Path logPath = Path.of("target", "test-audit", "incident-case-test.jsonl");
        Files.deleteIfExists(logPath);

        ActorRef<AuditActor.Command> auditActor = testKit.spawn(
                AuditActor.create(logPath)
        );
        ActorRef<RiskActor.Command> riskActor = testKit.spawn(RiskActor.create(new RiskRules(), auditActor));
        ActorRef<HistoryActor.Command> historyActor = testKit.spawn(HistoryActor.create(new HistoryService(), auditActor));
        ActorRef<PolicyActor.Command> policyActor = testKit.spawn(PolicyActor.create(new PolicyRules(), auditActor));
        ActorRef<LLMAnalysisActor.Command> llmActor = testKit.spawn(LLMAnalysisActor.create(new MockLlmProvider(), auditActor));
        ActorRef<DecisionActor.Command> decisionActor = testKit.spawn(DecisionActor.create(new DecisionService(), auditActor));
        ActorRef<EscalationActor.Command> escalationActor = testKit.spawn(EscalationActor.create(new EscalationRules(), auditActor));

        IncidentCaseActor.Dependencies dependencies = new IncidentCaseActor.Dependencies(
                riskActor,
                historyActor,
                policyActor,
                llmActor,
                decisionActor,
                escalationActor,
                auditActor
        );

        TestProbe<IncidentCaseResult> probe = testKit.createTestProbe(IncidentCaseResult.class);

        NormalizedIncident incident = new NormalizedIncident(
                "EVT-1001",
                "VitalsMonitor",
                "oxygen_drop",
                Severity.HIGH,
                "SpO2 < 88% for 40s",
                Instant.now()
        );

        testKit.spawn(IncidentCaseActor.create(incident, dependencies, probe.getRef()));

        IncidentCaseResult result = probe.receiveMessage();

        assertEquals("EVT-1001", result.incident().eventId());
        assertEquals(Severity.CRITICAL, result.riskResult().riskLevel());
        assertEquals(Severity.CRITICAL, result.finalDecision().finalSeverity());
        assertTrue(result.finalDecision().llmAccepted());
        assertEquals("Senior Doctor", result.escalationResult().target().displayName());
        assertEquals("CASE_COMPLETED", result.auditEvent().action());

        assertFileEventuallyContains(logPath, "\"action\":\"INCIDENT_CASE_ACTOR_CREATED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"CASE_PROCESSING_STARTED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"RISK_REQUESTED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"RISK_ACTOR_RECEIVED_ANALYZE_RISK\"");
        assertFileEventuallyContains(logPath, "\"action\":\"HISTORY_REQUESTED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"HISTORY_ACTOR_RECEIVED_ANALYZE_HISTORY\"");
        assertFileEventuallyContains(logPath, "\"action\":\"RISK_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"HISTORY_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"POLICY_ACTOR_RECEIVED_EVALUATE_POLICY\"");
        assertFileEventuallyContains(logPath, "\"action\":\"POLICY_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"LLM_ANALYSIS_ACTOR_RECEIVED_ANALYZE_INCIDENT_CONTEXT\"");
        assertFileEventuallyContains(logPath, "\"action\":\"LLM_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"DECISION_ACTOR_RECEIVED_MAKE_DECISION\"");
        assertFileEventuallyContains(logPath, "\"action\":\"DECISION_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"ESCALATION_ACTOR_RECEIVED_ROUTE_DECISION\"");
        assertFileEventuallyContains(logPath, "\"action\":\"ESCALATION_COMPLETED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"CASE_COMPLETED\"");
    }

    private void assertFileEventuallyContains(Path path, String expectedText) throws IOException {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(2).toNanos();
        String contents = "";

        while (System.nanoTime() < deadline) {
            if (Files.exists(path)) {
                contents = Files.readString(path);
                if (contents.contains(expectedText)) {
                    return;
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for trace log assertion: " + expectedText);
            }
        }

        fail("Did not find expected trace text in log file: " + expectedText + System.lineSeparator() + contents);
    }
}
