package org.example.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.config.ApplicationConfig;
import org.example.model.IncidentCaseResult;
import org.example.model.RawIncident;
import org.example.service.DecisionService;
import org.example.service.EscalationRules;
import org.example.service.HistoryService;
import org.example.service.IncidentRegistry;
import org.example.service.IncidentNormalizer;
import org.example.service.LlmProvider;
import org.example.service.LlmProviderFactory;
import org.example.service.MockLlmProvider;
import org.example.service.PolicyRules;
import org.example.service.RiskRules;

import java.util.List;

/**
 * Small root actor that wires the full demo pipeline together.
 * It creates the worker actors, starts the simulator, prints completed case
 * results, and terminates the actor system after the sample demo finishes.
 */
public class RootActor extends AbstractBehavior<RootActor.Command> {

    /**
     * Every message sent to RootActor must implement Command
     */
    public interface Command {
    }

    /**
     * This actor can receive a “start demo” request message
     */
    private record StartDemo() implements Command {
    }

    /**
     * This actor can receive a request containing one incident
     */
    public record SubmitRawIncident(RawIncident rawIncident) implements Command {
    }

    /**
     * This actor can receive a completed result
     */
    private record WrappedCaseResult(IncidentCaseResult result) implements Command {
    }
    // tools:
    private final ApplicationConfig config;
    private final IncidentRegistry incidentRegistry;
    private final ActorRef<IngestionActor.Command> ingestionActor;
    private final ActorRef<IncidentSimulatorActor.Command> simulatorActor;
    private final List<RawIncident> demoIncidents;
    private int completedResults = 0;

    /**
     * Creates the root actor behavior.
     * Use the default demo configuration if someone wants to create RootActor without passing any arguments
     */
    public static Behavior<Command> create() {
        return create(ApplicationConfig.demoDefaults(), new IncidentRegistry());
    }

    /**
     * Create RootActor
     * Method Overloading
     */
    public static Behavior<Command> create(ApplicationConfig config, IncidentRegistry incidentRegistry) {
        return Behaviors.setup(context -> new RootActor(context, config, incidentRegistry));
    }
    // constructor
    private RootActor(
            ActorContext<Command> context,
            ApplicationConfig config,
            IncidentRegistry incidentRegistry
    ) {
        super(context);
        this.config = config;
        this.incidentRegistry = incidentRegistry;
        // When the RootActor is created, the AuditActor needs to be created right away
        ActorRef<AuditActor.Command> auditActor = context.spawn(
                AuditActor.create(),
                "audit-actor"
        );
        // Choose which LLM provider to use, based on the configuration. Mock LLM / Real LLM
        LlmProvider llmProvider = config.llmMode() == ApplicationConfig.LlmMode.MOCK
                ? new MockLlmProvider()
                : LlmProviderFactory.createProvider(config);

        /*
         * Shared worker actors are created once here and reused by every
         * incident case.
         */
        ActorRef<RiskActor.Command> riskActor = context.spawn(
                RiskActor.create(new RiskRules(), auditActor),
                "risk-actor"
        );
        ActorRef<HistoryActor.Command> historyActor = context.spawn(
                HistoryActor.create(new HistoryService(), auditActor),
                "history-actor"
        );
        ActorRef<PolicyActor.Command> policyActor = context.spawn(
                PolicyActor.create(new PolicyRules(), auditActor),
                "policy-actor"
        );
        ActorRef<LLMAnalysisActor.Command> llmAnalysisActor = context.spawn(
                LLMAnalysisActor.create(llmProvider, auditActor),
                "llm-analysis-actor"
        );
        ActorRef<DecisionActor.Command> decisionActor = context.spawn(
                DecisionActor.create(new DecisionService(), auditActor),
                "decision-actor"
        );
        ActorRef<EscalationActor.Command> escalationActor = context.spawn(
                EscalationActor.create(new EscalationRules(), auditActor),
                "escalation-actor"
        );
        //Group other needed actors as dependencies(folder of useful contacts of each actor)
        IncidentCaseActor.Dependencies dependencies = new IncidentCaseActor.Dependencies(
                riskActor,
                historyActor,
                policyActor,
                llmAnalysisActor,
                decisionActor,
                escalationActor,
                auditActor
        );
        // Adapted-Response: Need to convert reply into RootActor's internal message type
        ActorRef<IncidentCaseResult> resultAdapter =
                context.messageAdapter(IncidentCaseResult.class, WrappedCaseResult::new);

        ActorRef<CaseManagerActor.Command> caseManagerActor = context.spawn(
                CaseManagerActor.create(dependencies, resultAdapter),
                "case-manager-actor"
        );

        this.ingestionActor = context.spawn(
                IngestionActor.create(new IncidentNormalizer(), caseManagerActor),
                "ingestion-actor"
        );

        this.simulatorActor = context.spawn(
                IncidentSimulatorActor.create(ingestionActor),
                "incident-simulator-actor"
        );

        this.demoIncidents = List.of(
                new RawIncident("EVT-1001", "VitalsMonitor", "oxygen_drop", "high", "SpO2 < 88% for 40s"),
                new RawIncident("EVT-1002", "VitalsMonitor", "sensor_dropout", "medium", "Sensor signal lost intermittently"),
                new RawIncident("EVT-1003", "InfusionPump", "dose_sync_failure", "high", "Dose update mismatch between modules"),
                new RawIncident("EVT-1004", "VitalsMonitor", "oxygen_drop", "high", "Repeated desaturation event")
        );
        // Send Message to Self: if demo mode is enabled, it sends itself StartDemo which createReceive() will catch it
        if (config.runDemoBatch()) {
            context.getSelf().tell(new StartDemo());
        }
    }

    // How actor react to messages(handler)
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartDemo.class, this::onStartDemo)
                .onMessage(SubmitRawIncident.class, this::onSubmitRawIncident)
                .onMessage(WrappedCaseResult.class, this::onWrappedCaseResult)
                .build();
    }

    /**
     * Starts the sample simulation.
     * Defines what RootActor does when it receives a StartDemo message
     */
    private Behavior<Command> onStartDemo(StartDemo command) {
        System.out.println("Starting triage simulation for " + demoIncidents.size() + " incidents.");
        simulatorActor.tell(new IncidentSimulatorActor.RunSimulation(demoIncidents));
        return this;
    }

    /**
     * Accepts one raw incident from the local UI server and feeds it into the
     * existing ingestion stage.
     * Defines what RootActor does when it receives a SubmitRawIncident message
     */
    private Behavior<Command> onSubmitRawIncident(SubmitRawIncident command) {
        ingestionActor.tell(new IngestionActor.SubmitIncident(command.rawIncident()));
        return this;
    }

    /**
     * Prints a compact summary for each completed incident and terminates the
     * actor system once every demo incident has finished.
     * Defines what RootActor does when it receives a WrappedCaseResult message
     */
    private Behavior<Command> onWrappedCaseResult(WrappedCaseResult command) {
        completedResults++;

        IncidentCaseResult result = command.result();
        incidentRegistry.recordCompletedResult(result);
        System.out.println(
                "Completed " + result.incident().eventId()
                        + " -> risk=" + result.riskResult().riskLevel()
                        + ", finalAction=" + result.finalDecision().finalAction()
                        + ", escalation=" + result.escalationResult().target().displayName()
        );
        // If all demo incidents are done, shut down the actor system
        if (config.runDemoBatch() && completedResults >= demoIncidents.size()) {
            System.out.println("Triage simulation finished.");
            getContext().getSystem().terminate();
        }

        return this;
    }
}
