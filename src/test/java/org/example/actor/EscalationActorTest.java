package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.EscalationResult;
import org.example.model.EscalationTarget;
import org.example.model.FinalAction;
import org.example.model.FinalDecision;
import org.example.model.Severity;
import org.example.service.EscalationRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscalationActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void escalationActorRoutesCriticalDecisionToSeniorDoctor() {
        ActorRef<EscalationActor.Command> escalationActor =
                testKit.spawn(EscalationActor.create(new EscalationRules()));
        TestProbe<EscalationResult> probe = testKit.createTestProbe(EscalationResult.class);

        FinalDecision finalDecision = new FinalDecision(
                "EVT-1001",
                Severity.CRITICAL,
                FinalAction.ESCALATE,
                true,
                true,
                "Critical incident",
                List.of("CRITICAL_REQUIRES_HUMAN_REVIEW")
        );

        escalationActor.tell(new EscalationActor.RouteDecision(finalDecision, probe.getRef()));

        EscalationResult result = probe.receiveMessage();

        assertEquals("EVT-1001", result.eventId());
        assertEquals(EscalationTarget.SENIOR_DOCTOR, result.target());
    }
}
