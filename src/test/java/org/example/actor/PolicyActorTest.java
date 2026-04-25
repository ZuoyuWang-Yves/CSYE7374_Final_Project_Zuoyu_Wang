package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.HistoryResult;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.example.service.PolicyRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void policyActorRepliesWithCriticalPolicies() {
        ActorRef<PolicyActor.Command> policyActor = testKit.spawn(PolicyActor.create(new PolicyRules()));
        TestProbe<PolicyResult> probe = testKit.createTestProbe(PolicyResult.class);

        RiskResult risk = new RiskResult(
                "EVT-1001",
                Severity.CRITICAL,
                "Critical risk"
        );
        HistoryResult history = new HistoryResult(
                "EVT-1001",
                1,
                false,
                "No repeated failure"
        );

        policyActor.tell(new PolicyActor.EvaluatePolicy(risk, history, probe.getRef()));

        PolicyResult result = probe.receiveMessage();

        assertTrue(result.humanReviewRequired());
        assertFalse(result.autoDismissAllowed());
        assertTrue(result.appliedPolicies().contains("NO_AUTO_DISMISS_CRITICAL"));
    }
}
