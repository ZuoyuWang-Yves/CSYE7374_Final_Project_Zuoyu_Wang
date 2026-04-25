package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.AuditEvent;
import org.example.model.EscalationResult;
import org.example.model.FinalDecision;
import org.example.model.HistoryResult;
import org.example.model.IncidentCaseResult;
import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;
import org.example.model.NormalizedIncident;
import org.example.model.PolicyResult;
import org.example.model.RiskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class IncidentCaseActor extends AbstractBehavior<IncidentCaseActor.Command> {

    public record Dependencies(
            ActorRef<RiskActor.Command> riskActor,
            ActorRef<HistoryActor.Command> historyActor,
            ActorRef<PolicyActor.Command> policyActor,
            ActorRef<LLMAnalysisActor.Command> llmAnalysisActor,
            ActorRef<DecisionActor.Command> decisionActor,
            ActorRef<EscalationActor.Command> escalationActor,
            ActorRef<AuditActor.Command> auditActor
    ) {
    }

    public interface Command {
    }

    private record StartProcessing() implements Command {
    }

    private record WrappedRiskResult(RiskResult riskResult) implements Command {
    }

    private record WrappedHistoryResult(HistoryResult historyResult) implements Command {
    }

    private record WrappedPolicyResult(PolicyResult policyResult) implements Command {
    }

    private record WrappedLlmAnalysis(LlmAnalysis llmAnalysis) implements Command {
    }

    private record WrappedFinalDecision(FinalDecision finalDecision) implements Command {
    }

    private record WrappedEscalationResult(EscalationResult escalationResult) implements Command {
    }

    private record WrappedAuditEvent(AuditEvent auditEvent) implements Command {
    }

    private final NormalizedIncident incident;
    private final Dependencies dependencies;
    private final ActorRef<IncidentCaseResult> replyTo;
    private final String caseActorName;

    private final ActorRef<RiskResult> riskResultAdapter;
    private final ActorRef<HistoryResult> historyResultAdapter;
    private final ActorRef<PolicyResult> policyResultAdapter;
    private final ActorRef<LlmAnalysis> llmAnalysisAdapter;
    private final ActorRef<FinalDecision> finalDecisionAdapter;
    private final ActorRef<EscalationResult> escalationResultAdapter;
    private final ActorRef<AuditEvent> auditEventAdapter;

    private RiskResult riskResult;
    private HistoryResult historyResult;
    private PolicyResult policyResult;
    private LlmAnalysis llmAnalysis;
    private FinalDecision finalDecision;
    private EscalationResult escalationResult;

    public static Behavior<Command> create(
            NormalizedIncident incident,
            Dependencies dependencies,
            ActorRef<IncidentCaseResult> replyTo
    ) {
        return Behaviors.setup(context -> new IncidentCaseActor(context, incident, dependencies, replyTo));
    }

    private IncidentCaseActor(
            ActorContext<Command> context,
            NormalizedIncident incident,
            Dependencies dependencies,
            ActorRef<IncidentCaseResult> replyTo
    ) {
        super(context);
        this.incident = incident;
        this.dependencies = dependencies;
        this.replyTo = replyTo;
        this.caseActorName = context.getSelf().path().name();

        this.riskResultAdapter = context.messageAdapter(RiskResult.class, WrappedRiskResult::new);
        this.historyResultAdapter = context.messageAdapter(HistoryResult.class, WrappedHistoryResult::new);
        this.policyResultAdapter = context.messageAdapter(PolicyResult.class, WrappedPolicyResult::new);
        this.llmAnalysisAdapter = context.messageAdapter(LlmAnalysis.class, WrappedLlmAnalysis::new);
        this.finalDecisionAdapter = context.messageAdapter(FinalDecision.class, WrappedFinalDecision::new);
        this.escalationResultAdapter = context.messageAdapter(EscalationResult.class, WrappedEscalationResult::new);
        this.auditEventAdapter = context.messageAdapter(AuditEvent.class, WrappedAuditEvent::new);

        writeTrace("IncidentCaseActor", "INCIDENT_CASE_ACTOR_CREATED", Map.of(
                "actorPath", context.getSelf().path().toString()
        ));
        context.getSelf().tell(new StartProcessing());
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartProcessing.class, this::onStartProcessing)
                .onMessage(WrappedRiskResult.class, this::onWrappedRiskResult)
                .onMessage(WrappedHistoryResult.class, this::onWrappedHistoryResult)
                .onMessage(WrappedPolicyResult.class, this::onWrappedPolicyResult)
                .onMessage(WrappedLlmAnalysis.class, this::onWrappedLlmAnalysis)
                .onMessage(WrappedFinalDecision.class, this::onWrappedFinalDecision)
                .onMessage(WrappedEscalationResult.class, this::onWrappedEscalationResult)
                .onMessage(WrappedAuditEvent.class, this::onWrappedAuditEvent)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onStartProcessing(StartProcessing command) {
        writeTrace("IncidentCaseActor", "CASE_PROCESSING_STARTED", Map.of(
                "eventType", incident.eventType(),
                "inputSeverity", incident.severity().name()
        ));
        writeTrace("IncidentCaseActor", "RISK_REQUESTED", Map.of(
                "targetActor", "RiskActor"
        ));
        writeTrace("IncidentCaseActor", "HISTORY_REQUESTED", Map.of(
                "targetActor", "HistoryActor"
        ));

        dependencies.riskActor().tell(new RiskActor.AnalyzeRisk(incident, riskResultAdapter));
        dependencies.historyActor().tell(new HistoryActor.AnalyzeHistory(incident, historyResultAdapter));
        return this;
    }

    private Behavior<Command> onWrappedRiskResult(WrappedRiskResult command) {
        this.riskResult = command.riskResult();
        writeTrace("RiskActor", "RISK_COMPLETED", Map.of(
                "riskLevel", riskResult.riskLevel().name(),
                "reason", riskResult.reason()
        ));
        requestPolicyWhenReady();
        return this;
    }

    private Behavior<Command> onWrappedHistoryResult(WrappedHistoryResult command) {
        this.historyResult = command.historyResult();
        writeTrace("HistoryActor", "HISTORY_COMPLETED", Map.of(
                "similarIncidentCount", historyResult.similarIncidentCount(),
                "repeatedFailure", historyResult.repeatedFailure()
        ));
        requestPolicyWhenReady();
        return this;
    }

    private void requestPolicyWhenReady() {
        if (riskResult == null || historyResult == null || policyResult != null) {
            return;
        }

        writeTrace("IncidentCaseActor", "POLICY_REQUESTED", Map.of(
                "targetActor", "PolicyActor",
                "riskLevel", riskResult.riskLevel().name(),
                "repeatedFailure", historyResult.repeatedFailure()
        ));

        dependencies.policyActor().tell(new PolicyActor.EvaluatePolicy(
                riskResult,
                historyResult,
                policyResultAdapter
        ));
    }

    private Behavior<Command> onWrappedPolicyResult(WrappedPolicyResult command) {
        this.policyResult = command.policyResult();
        writeTrace("PolicyActor", "POLICY_COMPLETED", Map.of(
                "humanReviewRequired", policyResult.humanReviewRequired(),
                "appliedPolicies", policyResult.appliedPolicies()
        ));

        IncidentContext incidentContext = new IncidentContext(
                incident,
                riskResult,
                historyResult,
                policyResult
        );

        writeTrace("IncidentCaseActor", "LLM_REQUESTED", Map.of(
                "targetActor", "LLMAnalysisActor",
                "riskLevel", riskResult.riskLevel().name(),
                "humanReviewRequired", policyResult.humanReviewRequired()
        ));

        dependencies.llmAnalysisActor().tell(new LLMAnalysisActor.AnalyzeIncidentContext(
                incidentContext,
                llmAnalysisAdapter
        ));
        return this;
    }

    private Behavior<Command> onWrappedLlmAnalysis(WrappedLlmAnalysis command) {
        this.llmAnalysis = command.llmAnalysis();
        writeTrace("LLMAnalysisActor", "LLM_COMPLETED", Map.of(
                "recommendedAction", llmAnalysis.recommendedAction(),
                "confidence", llmAnalysis.confidence(),
                "valid", llmAnalysis.valid()
        ));

        writeTrace("IncidentCaseActor", "DECISION_REQUESTED", Map.of(
                "targetActor", "DecisionActor",
                "llmValid", llmAnalysis.valid(),
                "recommendedAction", llmAnalysis.recommendedAction()
        ));

        dependencies.decisionActor().tell(new DecisionActor.MakeDecision(
                riskResult,
                policyResult,
                llmAnalysis,
                finalDecisionAdapter
        ));
        return this;
    }

    private Behavior<Command> onWrappedFinalDecision(WrappedFinalDecision command) {
        this.finalDecision = command.finalDecision();
        writeTrace("DecisionActor", "DECISION_COMPLETED", Map.of(
                "finalSeverity", finalDecision.finalSeverity().name(),
                "finalAction", finalDecision.finalAction().name(),
                "llmAccepted", finalDecision.llmAccepted(),
                "humanReviewRequired", finalDecision.humanReviewRequired()
        ));

        writeTrace("IncidentCaseActor", "ESCALATION_REQUESTED", Map.of(
                "targetActor", "EscalationActor",
                "finalSeverity", finalDecision.finalSeverity().name()
        ));

        dependencies.escalationActor().tell(new EscalationActor.RouteDecision(
                finalDecision,
                escalationResultAdapter
        ));
        return this;
    }

    private Behavior<Command> onWrappedEscalationResult(WrappedEscalationResult command) {
        this.escalationResult = command.escalationResult();
        writeTrace("EscalationActor", "ESCALATION_COMPLETED", Map.of(
                "escalationTarget", escalationResult.target().displayName()
        ));

        dependencies.auditActor().tell(new AuditActor.CreateAuditEvent(
                incident.eventId(),
                "IncidentCaseActor",
                "CASE_COMPLETED",
                buildAuditDetails(),
                auditEventAdapter
        ));
        return this;
    }

    private Map<String, Object> buildAuditDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("caseActorName", caseActorName);
        details.put("eventType", incident.eventType());
        details.put("inputSeverity", incident.severity().name());
        details.put("riskLevel", riskResult.riskLevel().name());
        details.put("repeatedFailure", historyResult.repeatedFailure());
        details.put("humanReviewRequired", policyResult.humanReviewRequired());
        details.put("llmAccepted", finalDecision.llmAccepted());
        details.put("finalSeverity", finalDecision.finalSeverity().name());
        details.put("finalAction", finalDecision.finalAction().name());
        details.put("escalationTarget", escalationResult.target().displayName());
        return details;
    }

    private void writeTrace(String actorName, String action, Map<String, Object> stageDetails) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("caseActorName", caseActorName);
        details.putAll(stageDetails);

        dependencies.auditActor().tell(new AuditActor.WriteAuditEvent(
                incident.eventId(),
                actorName,
                action,
                details
        ));
    }

    private Behavior<Command> onWrappedAuditEvent(WrappedAuditEvent command) {
        IncidentCaseResult result = new IncidentCaseResult(
                incident,
                riskResult,
                historyResult,
                policyResult,
                llmAnalysis,
                finalDecision,
                escalationResult,
                command.auditEvent()
        );

        replyTo.tell(result);
        return Behaviors.stopped();
    }

    private Behavior<Command> onPostStop() {
        writeTrace("IncidentCaseActor", "INCIDENT_CASE_ACTOR_TERMINATED", Map.of(
                "actorPath", getContext().getSelf().path().toString()
        ));
        return this;
    }
}
