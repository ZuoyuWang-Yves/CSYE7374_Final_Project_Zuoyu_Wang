package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.IncidentCaseResult;
import org.example.model.RawIncident;
import org.example.service.DecisionService;
import org.example.service.EscalationRules;
import org.example.service.HistoryService;
import org.example.service.IncidentNormalizer;
import org.example.service.MockLlmProvider;
import org.example.service.PolicyRules;
import org.example.service.RiskRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ActorFlowTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void rawIncidentFlowsThroughIngestionAndCaseManagement() throws IOException {
        Path logPath = Path.of("target", "test-audit", "actor-flow-test.jsonl");
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

        TestProbe<IncidentCaseResult> resultProbe = testKit.createTestProbe(IncidentCaseResult.class);

        ActorRef<CaseManagerActor.Command> caseManagerActor = testKit.spawn(
                CaseManagerActor.create(dependencies, resultProbe.getRef())
        );
        ActorRef<IngestionActor.Command> ingestionActor = testKit.spawn(
                IngestionActor.create(new IncidentNormalizer(), caseManagerActor)
        );

        ingestionActor.tell(new IngestionActor.SubmitIncident(
                new RawIncident(
                        "EVT-2001",
                        "InfusionPump",
                        "dose_sync_failure",
                        "high",
                        "Dose update mismatch"
                )
        ));

        IncidentCaseResult result = resultProbe.receiveMessage();

        assertEquals("EVT-2001", result.incident().eventId());
        assertEquals("dose_sync_failure", result.incident().eventType());
        assertEquals("Specialist", result.escalationResult().target().displayName());
        assertTrue(result.auditEvent().details().containsKey("finalAction"));

        assertFileEventuallyContains(logPath, "\"action\":\"CASE_OPENED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"INCIDENT_CASE_ACTOR_CREATED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"RISK_REQUESTED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"RISK_ACTOR_RECEIVED_ANALYZE_RISK\"");
        assertFileEventuallyContains(logPath, "\"action\":\"HISTORY_REQUESTED\"");
        assertFileEventuallyContains(logPath, "\"action\":\"HISTORY_ACTOR_RECEIVED_ANALYZE_HISTORY\"");
        assertFileEventuallyContains(logPath, "\"caseActorName\"");
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
