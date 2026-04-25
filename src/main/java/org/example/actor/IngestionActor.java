package org.example.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.NormalizedIncident;
import org.example.model.RawIncident;
import org.example.service.IncidentNormalizer;

public class IngestionActor extends AbstractBehavior<IngestionActor.Command> {

    public interface Command {
    }

    public record SubmitIncident(RawIncident rawIncident) implements Command {
    }
    private final IncidentNormalizer incidentNormalizer;
    private final akka.actor.typed.ActorRef<CaseManagerActor.Command> caseManagerActor;

    public static Behavior<Command> create(
            IncidentNormalizer incidentNormalizer,
            akka.actor.typed.ActorRef<CaseManagerActor.Command> caseManagerActor
    ) {
        return Behaviors.setup(context -> new IngestionActor(context, incidentNormalizer, caseManagerActor));
    }
    private IngestionActor(
            ActorContext<Command> context,
            IncidentNormalizer incidentNormalizer,
            akka.actor.typed.ActorRef<CaseManagerActor.Command> caseManagerActor
    ) {
        super(context);
        this.incidentNormalizer = incidentNormalizer;
        this.caseManagerActor = caseManagerActor;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SubmitIncident.class, this::onSubmitIncident)
                .build();
    }

    private Behavior<Command> onSubmitIncident(SubmitIncident command) {
        try {
            NormalizedIncident normalizedIncident = incidentNormalizer.normalize(command.rawIncident());
            caseManagerActor.tell(new CaseManagerActor.OpenCase(normalizedIncident));
        } catch (IllegalArgumentException exception) {
            System.err.println("Rejected raw incident: " + exception.getMessage());
        }

        return this;
    }
}
