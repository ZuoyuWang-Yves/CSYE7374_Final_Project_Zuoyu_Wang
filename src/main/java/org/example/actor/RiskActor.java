package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.NormalizedIncident;
import org.example.model.RiskResult;
import org.example.service.RiskRules;

import java.util.LinkedHashMap;
import java.util.Map;

public class RiskActor extends AbstractBehavior<RiskActor.Command> {

    public interface Command {
    }

    public record AnalyzeRisk(
            NormalizedIncident incident,
            ActorRef<RiskResult> replyTo
    ) implements Command {
    }

    private final RiskRules riskRules;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(RiskRules riskRules) {
        return create(riskRules, null);
    }

    public static Behavior<Command> create(
            RiskRules riskRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new RiskActor(context, riskRules, auditActor));
    }

    private RiskActor(
            ActorContext<Command> context,
            RiskRules riskRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.riskRules = riskRules;
        this.auditActor = auditActor;
        writeSystemTrace("RISK_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AnalyzeRisk.class, this::onAnalyzeRisk)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onAnalyzeRisk(AnalyzeRisk command) {
        writeIncidentTrace(command.incident(), "RISK_ACTOR_RECEIVED_ANALYZE_RISK", Map.of(
                "inputSeverity", command.incident().severity().name(),
                "eventType", command.incident().eventType()
        ));
        RiskResult result = riskRules.analyze(command.incident());
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
                "RiskActor",
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
                "RiskActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("RISK_ACTOR_TERMINATED");
        return this;
    }
}
