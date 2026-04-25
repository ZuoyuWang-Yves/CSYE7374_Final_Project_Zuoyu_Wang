package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.HistoryResult;
import org.example.model.NormalizedIncident;
import org.example.service.HistoryService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This actor looks at recent incident history and checks for repeated failure.
 */
public class HistoryActor extends AbstractBehavior<HistoryActor.Command> {

    /**
     * Every message sent to HistoryActor must implement Command.
     */
    public interface Command {
    }

    /**
     * This actor can receive one incident and analyze its history.
     */
    public record AnalyzeHistory(
            NormalizedIncident incident,
            ActorRef<HistoryResult> replyTo
    ) implements Command {
    }

    private final HistoryService historyService;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(HistoryService historyService) {
        return create(historyService, null);
    }

    public static Behavior<Command> create(
            HistoryService historyService,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new HistoryActor(context, historyService, auditActor));
    }

    private HistoryActor(
            ActorContext<Command> context,
            HistoryService historyService,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.historyService = historyService;
        this.auditActor = auditActor;
        writeSystemTrace("HISTORY_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AnalyzeHistory.class, this::onAnalyzeHistory)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onAnalyzeHistory(AnalyzeHistory command) {
        writeIncidentTrace(command.incident(), "HISTORY_ACTOR_RECEIVED_ANALYZE_HISTORY", Map.of(
                "inputSeverity", command.incident().severity().name(),
                "eventType", command.incident().eventType()
        ));
        HistoryResult result = historyService.analyze(command.incident());
        command.replyTo().tell(result);
        return this;
    }

    private void writeSystemTrace(String action) {
        if (auditActor == null) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actorPath", getContext().getSelf().path().toString());

        auditActor.tell(new AuditActor.WriteAuditEvent(
                AuditActor.SYSTEM_EVENT_ID,
                "HistoryActor",
                action,
                details
        ));
    }

    private void writeIncidentTrace(NormalizedIncident incident, String action, Map<String, Object> stageDetails) {
        if (auditActor == null) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.putAll(stageDetails);

        auditActor.tell(new AuditActor.WriteAuditEvent(
                incident.eventId(),
                "HistoryActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("HISTORY_ACTOR_TERMINATED");
        return this;
    }
}
