package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.FinalAction;
import org.example.model.FinalDecision;
import org.example.model.LlmAnalysis;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.example.service.DecisionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DecisionActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void decisionActorReturnsDeterministicFallbackWhenLlmIsInvalid() {
        ActorRef<DecisionActor.Command> decisionActor =
                testKit.spawn(DecisionActor.create(new DecisionService()));
        TestProbe<FinalDecision> probe = testKit.createTestProbe(FinalDecision.class);

        RiskResult risk = new RiskResult("EVT-1001", Severity.HIGH, "High risk");
        PolicyResult policy = new PolicyResult(
                "EVT-1001",
                false,
                false,
                List.of("NO_AUTO_DISMISS_HIGH_RISK")
        );
        LlmAnalysis invalidAnalysis = new LlmAnalysis(
                "EVT-1001",
                "",
                "",
                0.0,
                false
        );

        decisionActor.tell(new DecisionActor.MakeDecision(risk, policy, invalidAnalysis, probe.getRef()));

        FinalDecision result = probe.receiveMessage();

        assertEquals("EVT-1001", result.eventId());
        assertEquals(FinalAction.ESCALATE, result.finalAction());
        assertFalse(result.llmAccepted());
    }
}
