package org.example.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.example.model.AuditEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditActorTest {

    private static final ActorTestKit testKit = ActorTestKit.create();

    @AfterAll
    static void shutdownTestKit() {
        testKit.shutdownTestKit();
    }

    @Test
    void auditActorCreatesStructuredAuditEventAndWritesJsonlFile() throws IOException {
        Path logPath = Path.of("target", "test-audit", "audit-actor-test.jsonl");
        Files.deleteIfExists(logPath);

        ActorRef<AuditActor.Command> auditActor = testKit.spawn(AuditActor.create(logPath));
        TestProbe<AuditEvent> probe = testKit.createTestProbe(AuditEvent.class);

        auditActor.tell(new AuditActor.CreateAuditEvent(
                "EVT-1001",
                "DecisionActor",
                "FINAL_DECISION_CREATED",
                Map.of("finalSeverity", "CRITICAL", "llmAccepted", true),
                probe.getRef()
        ));

        AuditEvent result = probe.receiveMessage();

        assertEquals("EVT-1001", result.eventId());
        assertEquals("DecisionActor", result.actorName());
        assertEquals("FINAL_DECISION_CREATED", result.action());
        assertTrue(result.details().containsKey("finalSeverity"));

        String fileContents = Files.readString(logPath);
        assertTrue(fileContents.contains("\"eventId\":\"EVT-1001\""));
        assertTrue(fileContents.contains("\"actorName\":\"DecisionActor\""));
        assertTrue(fileContents.contains("\"action\":\"FINAL_DECISION_CREATED\""));
    }

    @Test
    void auditActorAppendsMultipleEventsAsSeparateJsonlLines() throws IOException {
        Path logPath = Path.of("target", "test-audit", "audit-actor-multi-test.jsonl");
        Files.deleteIfExists(logPath);

        ActorRef<AuditActor.Command> auditActor = testKit.spawn(AuditActor.create(logPath));
        TestProbe<AuditEvent> probe = testKit.createTestProbe(AuditEvent.class);

        auditActor.tell(new AuditActor.CreateAuditEvent(
                "EVT-2001",
                "IncidentCaseActor",
                "CASE_COMPLETED",
                Map.of("finalSeverity", "HIGH"),
                probe.getRef()
        ));
        probe.receiveMessage();

        auditActor.tell(new AuditActor.CreateAuditEvent(
                "EVT-2002",
                "IncidentCaseActor",
                "CASE_COMPLETED",
                Map.of("finalSeverity", "LOW"),
                probe.getRef()
        ));
        probe.receiveMessage();

        java.util.List<String> lines = Files.readAllLines(logPath);

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"eventId\":\"EVT-2001\""));
        assertTrue(lines.get(1).contains("\"eventId\":\"EVT-2002\""));
    }
}
