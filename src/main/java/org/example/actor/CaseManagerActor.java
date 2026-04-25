package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.IncidentCaseResult;
import org.example.model.NormalizedIncident;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

/**
 * This actor opens one case actor for each incident and tracks active cases.
 */
public class CaseManagerActor extends AbstractBehavior<CaseManagerActor.Command> {

    /**
     * Every message sent to CaseManagerActor must implement Command.
     */
    public interface Command {
    }

    /**
     * This actor can receive a normalized incident and open a case for it.
     */
    public record OpenCase(NormalizedIncident incident) implements Command {
    }

    /**
     * This actor can receive a request for the current active case count.
     */
    public record GetActiveCaseCount(ActorRef<Integer> replyTo) implements Command {
    }

    private record WrappedCaseCompleted(IncidentCaseResult result) implements Command {
    }
    private final IncidentCaseActor.Dependencies dependencies;
    private final ActorRef<IncidentCaseResult> resultSink;
    private final ActorRef<IncidentCaseResult> caseCompletedAdapter;
    private final Map<String, ActorRef<IncidentCaseActor.Command>> activeCasesByEventId = new HashMap<>();
    private long caseSequence = 0L; //counter for how many IncidentCaseActor created

    public static Behavior<Command> create(
            IncidentCaseActor.Dependencies dependencies,
            ActorRef<IncidentCaseResult> resultSink
    ) {
        return Behaviors.setup(context -> new CaseManagerActor(context, dependencies, resultSink));
    }

    private CaseManagerActor(
            ActorContext<Command> context,
            IncidentCaseActor.Dependencies dependencies,
            ActorRef<IncidentCaseResult> resultSink
    ) {
        super(context);
        this.dependencies = dependencies;
        this.resultSink = resultSink;
        this.caseCompletedAdapter = context.messageAdapter(IncidentCaseResult.class, WrappedCaseCompleted::new);
        writeLifecycleTrace("CASE_MANAGER_ACTOR_CREATED");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(OpenCase.class, this::onOpenCase)
                .onMessage(GetActiveCaseCount.class, this::onGetActiveCaseCount)
                .onMessage(WrappedCaseCompleted.class, this::onWrappedCaseCompleted)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onOpenCase(OpenCase command) {
        NormalizedIncident incident = command.incident();

        caseSequence++;
        String actorName = buildCaseActorName(incident.eventId(), caseSequence);

        ActorRef<IncidentCaseActor.Command> caseActor = getContext().spawn(
                IncidentCaseActor.create(incident, dependencies, caseCompletedAdapter),
                actorName
        );

        activeCasesByEventId.put(incident.eventId(), caseActor);
        writeCaseOpenedTrace(incident, actorName);
        return this;
    }

    private String buildCaseActorName(String eventId, long sequence) {
        String sanitizedEventId = eventId.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return "incident-case-" + sanitizedEventId + "-" + sequence;
    }

    private Behavior<Command> onGetActiveCaseCount(GetActiveCaseCount command) {
        command.replyTo().tell(activeCasesByEventId.size());
        return this;
    }

    private Behavior<Command> onWrappedCaseCompleted(WrappedCaseCompleted command) {
        activeCasesByEventId.remove(command.result().incident().eventId());
        resultSink.tell(command.result());
        return this;
    }

    private void writeCaseOpenedTrace(NormalizedIncident incident, String actorName) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("caseActorName", actorName);
        details.put("eventType", incident.eventType());
        details.put("inputSeverity", incident.severity().name());
        details.put("activeCaseCount", activeCasesByEventId.size());

        dependencies.auditActor().tell(new AuditActor.WriteAuditEvent(
                incident.eventId(),
                "CaseManagerActor",
                "CASE_OPENED",
                details
        ));
    }

    private void writeLifecycleTrace(String action) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("actorPath", getContext().getSelf().path().toString());

        dependencies.auditActor().tell(new AuditActor.WriteAuditEvent(
                AuditActor.SYSTEM_EVENT_ID,
                "CaseManagerActor",
                action,
                details
        ));
    }
    private Behavior<Command> onPostStop() {
        writeLifecycleTrace("CASE_MANAGER_ACTOR_TERMINATED");
        return this;
    }
}
