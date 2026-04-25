package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.EscalationResult;
import org.example.model.FinalDecision;
import org.example.service.EscalationRules;

import java.util.LinkedHashMap;
import java.util.Map;

public class EscalationActor extends AbstractBehavior<EscalationActor.Command> {

    public interface Command {
    }

    public record RouteDecision(
            FinalDecision finalDecision,
            ActorRef<EscalationResult> replyTo
    ) implements Command {
    }

    private final EscalationRules escalationRules;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(EscalationRules escalationRules) {
        return create(escalationRules, null);
    }

    public static Behavior<Command> create(
            EscalationRules escalationRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new EscalationActor(context, escalationRules, auditActor));
    }

    private EscalationActor(
            ActorContext<Command> context,
            EscalationRules escalationRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.escalationRules = escalationRules;
        this.auditActor = auditActor;
        writeSystemTrace("ESCALATION_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RouteDecision.class, this::onRouteDecision)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onRouteDecision(RouteDecision command) {
        writeIncidentTrace(command.finalDecision().eventId(), "ESCALATION_ACTOR_RECEIVED_ROUTE_DECISION", Map.of(
                "finalSeverity", command.finalDecision().finalSeverity().name(),
                "finalAction", command.finalDecision().finalAction().name()
        ));
        EscalationResult result = escalationRules.route(command.finalDecision());
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
                "EscalationActor",
                action,
                details
        ));
    }

    private void writeIncidentTrace(String eventId, String action, Map<String, Object> stageDetails) {
        if (auditActor == null) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.putAll(stageDetails);

        auditActor.tell(new AuditActor.WriteAuditEvent(
                eventId,
                "EscalationActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("ESCALATION_ACTOR_TERMINATED");
        return this;
    }
}
