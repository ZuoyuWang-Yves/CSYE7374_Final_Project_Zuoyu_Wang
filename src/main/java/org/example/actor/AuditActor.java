package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.AuditEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public class AuditActor extends AbstractBehavior<AuditActor.Command> {

    public static final String SYSTEM_EVENT_ID = "SYSTEM";

    private static final ZoneId EASTERN_TIME_ZONE = ZoneId.of("America/New_York");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public interface Command {
    }

    public record WriteAuditEvent(
            String eventId,
            String actorName,
            String action,
            Map<String, Object> details
    ) implements Command {
    }

    public record CreateAuditEvent(
            String eventId,
            String actorName,
            String action,
            Map<String, Object> details,
            ActorRef<AuditEvent> replyTo
    ) implements Command {
    }

    private final Path logPath;

    public static Behavior<Command> create() {
        return create(Path.of("logs", "audit.jsonl"));
    }

    public static Behavior<Command> create(Path logPath) {
        return Behaviors.setup(context -> new AuditActor(context, logPath));
    }

    private AuditActor(ActorContext<Command> context, Path logPath) {
        super(context);
        this.logPath = logPath;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(WriteAuditEvent.class, this::onWriteAuditEvent)
                .onMessage(CreateAuditEvent.class, this::onCreateAuditEvent)
                .build();
    }

    private Behavior<Command> onWriteAuditEvent(WriteAuditEvent command) {
        AuditEvent auditEvent = buildAuditEvent(
                command.eventId(),
                command.actorName(),
                command.action(),
                command.details()
        );
        appendJsonLine(auditEvent);
        return this;
    }

    private Behavior<Command> onCreateAuditEvent(CreateAuditEvent command) {
        AuditEvent auditEvent = buildAuditEvent(
                command.eventId(),
                command.actorName(),
                command.action(),
                command.details()
        );

        appendJsonLine(auditEvent);
        command.replyTo().tell(auditEvent);
        return this;
    }

    private AuditEvent buildAuditEvent(
            String eventId,
            String actorName,
            String action,
            Map<String, Object> details
    ) {
        Instant now = Instant.now();

        return new AuditEvent(
                now,
                now.atZone(EASTERN_TIME_ZONE),
                eventId,
                actorName,
                action,
                Map.copyOf(details)
        );
    }

    private void appendJsonLine(AuditEvent auditEvent) {
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String jsonLine = OBJECT_MAPPER.writeValueAsString(auditEvent) + System.lineSeparator();
            Files.writeString(
                    logPath,
                    jsonLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write audit log to " + logPath, exception);
        }
    }
}
