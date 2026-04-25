package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.FinalDecision;
import org.example.model.LlmAnalysis;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.service.DecisionService;

import java.util.LinkedHashMap;
import java.util.Map;

public class DecisionActor extends AbstractBehavior<DecisionActor.Command> {

    public interface Command {
    }

    public record MakeDecision(
            RiskResult riskResult,
            PolicyResult policyResult,
            LlmAnalysis llmAnalysis,
            ActorRef<FinalDecision> replyTo
    ) implements Command {
    }

    private final DecisionService decisionService;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(DecisionService decisionService) {
        return create(decisionService, null);
    }

    public static Behavior<Command> create(
            DecisionService decisionService,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new DecisionActor(context, decisionService, auditActor));
    }

    private DecisionActor(
            ActorContext<Command> context,
            DecisionService decisionService,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.decisionService = decisionService;
        this.auditActor = auditActor;
        writeSystemTrace("DECISION_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MakeDecision.class, this::onMakeDecision)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onMakeDecision(MakeDecision command) {
        writeIncidentTrace(command.riskResult().eventId(), "DECISION_ACTOR_RECEIVED_MAKE_DECISION", Map.of(
                "riskLevel", command.riskResult().riskLevel().name(),
                "llmValid", command.llmAnalysis() != null && command.llmAnalysis().valid(),
                "humanReviewRequired", command.policyResult().humanReviewRequired()
        ));
        FinalDecision finalDecision = decisionService.decide(
                command.riskResult(),
                command.policyResult(),
                command.llmAnalysis()
        );
        command.replyTo().tell(finalDecision);
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
                "DecisionActor",
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
                "DecisionActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("DECISION_ACTOR_TERMINATED");
        return this;
    }
}
