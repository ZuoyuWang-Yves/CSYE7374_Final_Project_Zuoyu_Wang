package org.example.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.model.RawIncident;

import java.util.List;

/**
 * This actor sends a batch of sample incidents into the pipeline.
 */
public class IncidentSimulatorActor extends AbstractBehavior<IncidentSimulatorActor.Command> {

    /**
     * Every message sent to IncidentSimulatorActor must implement Command.
     */
    public interface Command {
    }

    /**
     * This actor can receive a list of demo incidents to submit.
     */
    public record RunSimulation(List<RawIncident> incidents) implements Command {
    }

    private final akka.actor.typed.ActorRef<IngestionActor.Command> ingestionActor;

    public static Behavior<Command> create(akka.actor.typed.ActorRef<IngestionActor.Command> ingestionActor) {
        return Behaviors.setup(context -> new IncidentSimulatorActor(context, ingestionActor));
    }

    private IncidentSimulatorActor(
            ActorContext<Command> context,
            akka.actor.typed.ActorRef<IngestionActor.Command> ingestionActor
    ) {
        super(context);
        this.ingestionActor = ingestionActor;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RunSimulation.class, this::onRunSimulation)
                .build();
    }

    private Behavior<Command> onRunSimulation(RunSimulation command) {
        for (RawIncident rawIncident : command.incidents()) {
            ingestionActor.tell(new IngestionActor.SubmitIncident(rawIncident));
        }
        return this;
    }
}
