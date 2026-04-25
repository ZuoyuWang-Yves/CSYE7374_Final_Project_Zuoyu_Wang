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

import java.nio.file.Path;
import java.time.Duration;

class IngestionActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void invalidRawIncidentIsRejectedAndProducesNoCaseResult() {
        ActorRef<RiskActor.Command> riskActor = testKit.spawn(RiskActor.create(new RiskRules()));
        ActorRef<HistoryActor.Command> historyActor = testKit.spawn(HistoryActor.create(new HistoryService()));
        ActorRef<PolicyActor.Command> policyActor = testKit.spawn(PolicyActor.create(new PolicyRules()));
        ActorRef<LLMAnalysisActor.Command> llmActor = testKit.spawn(LLMAnalysisActor.create(new MockLlmProvider()));
        ActorRef<DecisionActor.Command> decisionActor = testKit.spawn(DecisionActor.create(new DecisionService()));
        ActorRef<EscalationActor.Command> escalationActor = testKit.spawn(EscalationActor.create(new EscalationRules()));
        ActorRef<AuditActor.Command> auditActor = testKit.spawn(
                AuditActor.create(Path.of("target", "test-audit", "ingestion-invalid-test.jsonl"))
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
        TestProbe<Integer> countProbe = testKit.createTestProbe(Integer.class);

        ActorRef<CaseManagerActor.Command> caseManagerActor = testKit.spawn(
                CaseManagerActor.create(dependencies, resultProbe.getRef())
        );
        ActorRef<IngestionActor.Command> ingestionActor = testKit.spawn(
                IngestionActor.create(new IncidentNormalizer(), caseManagerActor)
        );

        ingestionActor.tell(new IngestionActor.SubmitIncident(
                new RawIncident(
                        "EVT-BAD-1",
                        " ",
                        "oxygen_drop",
                        "high",
                        "Missing system should be rejected"
                )
        ));

        resultProbe.expectNoMessage(Duration.ofMillis(300));

        caseManagerActor.tell(new CaseManagerActor.GetActiveCaseCount(countProbe.getRef()));
        countProbe.expectMessage(0);
    }
}
