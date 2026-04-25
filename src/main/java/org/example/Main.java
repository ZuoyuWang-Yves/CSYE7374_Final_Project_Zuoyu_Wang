package org.example;

import akka.actor.typed.ActorSystem;
import org.example.actor.RootActor;
import org.example.config.ApplicationConfig;
import org.example.service.IncidentQuestionService;
import org.example.service.IncidentRegistry;
import org.example.service.TraceRepository;
import org.example.ui.LocalUiServer;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        ApplicationConfig config = ApplicationConfig.fromEnvironment(args);
        IncidentRegistry incidentRegistry = new IncidentRegistry();

        ActorSystem<RootActor.Command> actorSystem =
                ActorSystem.create(
                        RootActor.create(config, incidentRegistry),
                        "medical-incident-triage-system"
                );

        LocalUiServer uiServer = null;
        if (config.runUiServer()) {
            uiServer = new LocalUiServer(
                    config,
                    actorSystem,
                    incidentRegistry,
                    new TraceRepository(Path.of("logs", "audit.jsonl")),
                    new IncidentQuestionService(config)
            );
            uiServer.start();
            System.out.println("UI server started at http://localhost:" + config.uiPort());
            System.out.println("LLM mode: " + config.llmModeLabel());
        }

        LocalUiServer serverToClose = uiServer;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serverToClose != null) {
                serverToClose.stop();
            }
        }));

        actorSystem.getWhenTerminated().toCompletableFuture().join();

        if (uiServer != null) {
            uiServer.stop();
        }
    }
}
