package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;
import org.example.service.LlmProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class LLMAnalysisActor extends AbstractBehavior<LLMAnalysisActor.Command> {

    public interface Command {
    }

    public record AnalyzeIncidentContext(
            IncidentContext context,
            ActorRef<LlmAnalysis> replyTo
    ) implements Command {
    }

    private final LlmProvider llmProvider;
    private final ActorRef<AuditActor.Command> auditActor;

    public static Behavior<Command> create(LlmProvider llmProvider) {
        return create(llmProvider, null);
    }

    public static Behavior<Command> create(
            LlmProvider llmProvider,
            ActorRef<AuditActor.Command> auditActor
    ) {
        return Behaviors.setup(context -> new LLMAnalysisActor(context, llmProvider, auditActor));
    }

    private LLMAnalysisActor(
            ActorContext<Command> context,
            LlmProvider llmProvider,
            ActorRef<AuditActor.Command> auditActor
    ) {
        super(context);
        this.llmProvider = llmProvider;
        this.auditActor = auditActor;
        writeSystemTrace("LLM_ANALYSIS_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AnalyzeIncidentContext.class, this::onAnalyzeIncidentContext)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onAnalyzeIncidentContext(AnalyzeIncidentContext command) {
        writeIncidentTrace(command.context().incident().eventId(), "LLM_ANALYSIS_ACTOR_RECEIVED_ANALYZE_INCIDENT_CONTEXT", Map.of(
                "riskLevel", command.context().risk().riskLevel().name(),
                "humanReviewRequired", command.context().policy().humanReviewRequired()
        ));
        LlmAnalysis analysis = llmProvider.analyze(command.context());
        command.replyTo().tell(analysis);
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
                "LLMAnalysisActor",
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
                "LLMAnalysisActor",
                action,
                details
        ));
    }

    private Behavior<Command> onPostStop() {
        writeSystemTrace("LLM_ANALYSIS_ACTOR_TERMINATED");
        return this;
    }
}
