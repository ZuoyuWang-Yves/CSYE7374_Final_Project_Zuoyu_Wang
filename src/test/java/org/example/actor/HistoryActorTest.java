package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.HistoryResult;
import org.example.model.NormalizedIncident;
import org.example.model.Severity;
import org.example.service.HistoryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void historyActorDetectsRepeatedFailureAfterThreeSimilarIncidents() {
        ActorRef<HistoryActor.Command> historyActor = testKit.spawn(HistoryActor.create(new HistoryService()));
        TestProbe<HistoryResult> probe = testKit.createTestProbe(HistoryResult.class);

        NormalizedIncident first = incident("EVT-1001");
        NormalizedIncident second = incident("EVT-1002");
        NormalizedIncident third = incident("EVT-1003");

        historyActor.tell(new HistoryActor.AnalyzeHistory(first, probe.getRef()));
        HistoryResult firstResult = probe.receiveMessage();

        historyActor.tell(new HistoryActor.AnalyzeHistory(second, probe.getRef()));
        probe.receiveMessage();

        historyActor.tell(new HistoryActor.AnalyzeHistory(third, probe.getRef()));
        HistoryResult thirdResult = probe.receiveMessage();

        assertFalse(firstResult.repeatedFailure());
        assertTrue(thirdResult.repeatedFailure());
    }

    private NormalizedIncident incident(String eventId) {
        return new NormalizedIncident(
                eventId,
                "VitalsMonitor",
                "sensor_dropout",
                Severity.MEDIUM,
                "Sensor signal lost",
                Instant.now()
        );
    }
}
