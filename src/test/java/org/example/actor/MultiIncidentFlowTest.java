package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.IncidentCaseResult;
import org.example.model.RawIncident;
import org.example.model.Severity;
import org.example.service.DecisionService;
import org.example.service.EscalationRules;
import org.example.service.HistoryService;
import org.example.service.IncidentNormalizer;
import org.example.service.MockLlmProvider;
import org.example.service.PolicyRules;
import org.example.service.RiskRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiIncidentFlowTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void simulatorProcessesSeveralIncidentsAndReturnsIndependentResults() {
        ActorRef<RiskActor.Command> riskActor = testKit.spawn(RiskActor.create(new RiskRules()));
        ActorRef<HistoryActor.Command> historyActor = testKit.spawn(HistoryActor.create(new HistoryService()));
        ActorRef<PolicyActor.Command> policyActor = testKit.spawn(PolicyActor.create(new PolicyRules()));
        ActorRef<LLMAnalysisActor.Command> llmActor = testKit.spawn(LLMAnalysisActor.create(new MockLlmProvider()));
        ActorRef<DecisionActor.Command> decisionActor = testKit.spawn(DecisionActor.create(new DecisionService()));
        ActorRef<EscalationActor.Command> escalationActor = testKit.spawn(EscalationActor.create(new EscalationRules()));
        ActorRef<AuditActor.Command> auditActor = testKit.spawn(
                AuditActor.create(Path.of("target", "test-audit", "multi-incident-test.jsonl"))
        );

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
        ActorRef<IncidentSimulatorActor.Command> simulatorActor = testKit.spawn(
                IncidentSimulatorActor.create(ingestionActor)
        );

        List<RawIncident> incidents = List.of(
                new RawIncident("EVT-3001", "VitalsMonitor", "oxygen_drop", "high", "SpO2 < 88% for 40s"),
                new RawIncident("EVT-3002", "VitalsMonitor", "sensor_dropout", "high", "Sensor signal lost"),
                new RawIncident("EVT-3003", "InfusionPump", "dose_sync_failure", "medium", "Dose mismatch"),
                new RawIncident("EVT-3004", "VitalsMonitor", "temperature_warning", "low", "Temperature slightly elevated")
        );

        simulatorActor.tell(new IncidentSimulatorActor.RunSimulation(incidents));

        Map<String, IncidentCaseResult> resultsByEventId = new HashMap<>();
        for (int index = 0; index < incidents.size(); index++) {
            IncidentCaseResult result = resultProbe.receiveMessage();
            resultsByEventId.put(result.incident().eventId(), result);
        }

        assertEquals(4, resultsByEventId.size());

        IncidentCaseResult oxygenDropResult = resultsByEventId.get("EVT-3001");
        assertEquals(Severity.CRITICAL, oxygenDropResult.finalDecision().finalSeverity());
        assertEquals("Senior Doctor", oxygenDropResult.escalationResult().target().displayName());

        IncidentCaseResult sensorDropoutResult = resultsByEventId.get("EVT-3002");
        assertEquals(Severity.HIGH, sensorDropoutResult.finalDecision().finalSeverity());
        assertEquals("Specialist", sensorDropoutResult.escalationResult().target().displayName());

        IncidentCaseResult doseSyncResult = resultsByEventId.get("EVT-3003");
        assertEquals(Severity.HIGH, doseSyncResult.finalDecision().finalSeverity());
        assertEquals("Specialist", doseSyncResult.escalationResult().target().displayName());

        IncidentCaseResult lowSeverityResult = resultsByEventId.get("EVT-3004");
        assertEquals(Severity.LOW, lowSeverityResult.finalDecision().finalSeverity());
        assertEquals("Nurse", lowSeverityResult.escalationResult().target().displayName());

        for (IncidentCaseResult result : resultsByEventId.values()) {
            assertTrue(result.auditEvent().details().containsKey("finalAction"));
            assertEquals(result.incident().eventId(), result.finalDecision().eventId());
        }
    }

    @Test
    void repeatedIncidentsTriggerHistoryAndHumanReviewOnLaterCases() {
        ActorRef<RiskActor.Command> riskActor = testKit.spawn(RiskActor.create(new RiskRules()));
        ActorRef<HistoryActor.Command> historyActor = testKit.spawn(HistoryActor.create(new HistoryService()));
        ActorRef<PolicyActor.Command> policyActor = testKit.spawn(PolicyActor.create(new PolicyRules()));
        ActorRef<LLMAnalysisActor.Command> llmActor = testKit.spawn(LLMAnalysisActor.create(new MockLlmProvider()));
        ActorRef<DecisionActor.Command> decisionActor = testKit.spawn(DecisionActor.create(new DecisionService()));
        ActorRef<EscalationActor.Command> escalationActor = testKit.spawn(EscalationActor.create(new EscalationRules()));
        ActorRef<AuditActor.Command> auditActor = testKit.spawn(
                AuditActor.create(Path.of("target", "test-audit", "repeated-history-test.jsonl"))
        );

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
        ActorRef<IncidentSimulatorActor.Command> simulatorActor = testKit.spawn(
                IncidentSimulatorActor.create(ingestionActor)
        );

        List<RawIncident> incidents = List.of(
                new RawIncident("EVT-4001", "VitalsMonitor", "sensor_dropout", "low", "First dropout"),
                new RawIncident("EVT-4002", "VitalsMonitor", "sensor_dropout", "low", "Second dropout"),
                new RawIncident("EVT-4003", "VitalsMonitor", "sensor_dropout", "low", "Third dropout")
        );

        simulatorActor.tell(new IncidentSimulatorActor.RunSimulation(incidents));

        Map<String, IncidentCaseResult> resultsByEventId = new HashMap<>();
        for (int index = 0; index < incidents.size(); index++) {
            IncidentCaseResult result = resultProbe.receiveMessage();
            resultsByEventId.put(result.incident().eventId(), result);
        }

        List<Integer> similarIncidentCounts = new ArrayList<>();
        int repeatedFailureCount = 0;
        int humanReviewCount = 0;

        for (IncidentCaseResult result : resultsByEventId.values()) {
            similarIncidentCounts.add(result.historyResult().similarIncidentCount());

            if (result.historyResult().repeatedFailure()) {
                repeatedFailureCount++;
                assertEquals(3, result.historyResult().similarIncidentCount());
            }

            if (result.policyResult().humanReviewRequired()) {
                humanReviewCount++;
                assertTrue(result.policyResult().appliedPolicies().contains("REPEATED_FAILURE_REQUIRES_ESCALATION"));
            }
        }

        similarIncidentCounts.sort(Integer::compareTo);

        assertEquals(List.of(1, 2, 3), similarIncidentCounts);
        assertEquals(1, repeatedFailureCount);
        assertEquals(1, humanReviewCount);
    }
}
