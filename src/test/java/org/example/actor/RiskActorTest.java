package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.NormalizedIncident;
import org.example.model.RiskResult;
import org.example.model.Severity;
import org.example.service.RiskRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void riskActorRepliesWithRiskResult() {
        ActorRef<RiskActor.Command> riskActor = testKit.spawn(RiskActor.create(new RiskRules()));
        TestProbe<RiskResult> probe = testKit.createTestProbe(RiskResult.class);

        NormalizedIncident incident = new NormalizedIncident(
                "EVT-1001",
                "VitalsMonitor",
                "oxygen_drop",
                Severity.HIGH,
                "SpO2 < 88% for 40s",
                Instant.now()
        );

        riskActor.tell(new RiskActor.AnalyzeRisk(incident, probe.getRef()));

        RiskResult result = probe.receiveMessage();

        assertEquals("EVT-1001", result.eventId());
        assertEquals(Severity.CRITICAL, result.riskLevel());
    }
}
