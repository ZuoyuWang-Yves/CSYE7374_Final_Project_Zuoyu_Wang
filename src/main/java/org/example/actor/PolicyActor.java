package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.HistoryResult;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;
import org.example.service.PolicyRules;

import java.util.LinkedHashMap;
import java.util.Map;

public class PolicyActor extends AbstractBehavior<PolicyActor.Command> {

    public interface Command {
    }

    public record EvaluatePolicy(
            RiskResult riskResult,
            HistoryResult historyResult,
            ActorRef<PolicyResult> replyTo
    ) implements Command {
    }

    private final PolicyRules policyRules;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(PolicyRules policyRules) {
        return create(policyRules, null);
    }

    public static Behavior<Command> create(
            PolicyRules policyRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new PolicyActor(context, policyRules, auditActor));
    }

    private PolicyActor(
            ActorContext<Command> context,
            PolicyRules policyRules,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.policyRules = policyRules;
        this.auditActor = auditActor;
        writeSystemTrace("POLICY_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(EvaluatePolicy.class, this::onEvaluatePolicy)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onEvaluatePolicy(EvaluatePolicy command) {
        writeIncidentTrace(command.riskResult().eventId(), "POLICY_ACTOR_RECEIVED_EVALUATE_POLICY", Map.of(
                "riskLevel", command.riskResult().riskLevel().name(),
                "repeatedFailure", command.historyResult().repeatedFailure()
        ));
        PolicyResult result = policyRules.evaluate(command.riskResult(), command.historyResult());
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
                "PolicyActor",
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
                "PolicyActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("POLICY_ACTOR_TERMINATED");
        return this;
    }
}
