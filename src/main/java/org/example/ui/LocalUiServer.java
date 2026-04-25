package org.example.ui;

import akka.actor.typed.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.actor.RootActor;
import org.example.config.ApplicationConfig;
import org.example.model.AuditEvent;
import org.example.model.IncidentCaseResult;
import org.example.model.QuestionAnswer;
import org.example.model.RawIncident;
import org.example.service.IncidentQuestionService;
import org.example.service.IncidentRegistry;
import org.example.service.TraceRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Small local HTTP server for the browser UI.
 */
public class LocalUiServer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ApplicationConfig config;
    private final ActorSystem<RootActor.Command> actorSystem;
    private final IncidentRegistry incidentRegistry;
    private final TraceRepository traceRepository;
    private final IncidentQuestionService incidentQuestionService;
    private final HttpServer httpServer;

    public LocalUiServer(
            ApplicationConfig config,
            ActorSystem<RootActor.Command> actorSystem,
            IncidentRegistry incidentRegistry,
            TraceRepository traceRepository,
            IncidentQuestionService incidentQuestionService
    ) {
        try {
            this.config = config;
            this.actorSystem = actorSystem;
            this.incidentRegistry = incidentRegistry;
            this.traceRepository = traceRepository;
            this.incidentQuestionService = incidentQuestionService;
            this.httpServer = HttpServer.create(new InetSocketAddress(config.uiPort()), 0);
            registerRoutes();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start local UI server", exception);
        }
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }

    private void registerRoutes() {
        httpServer.createContext("/", this::handleIndexPage);
        httpServer.createContext("/api/meta", this::handleMeta);
        httpServer.createContext("/api/incidents", this::handleIncidents);
        httpServer.createContext("/api/incidents/", this::handleIncidentById);
        httpServer.createContext("/api/traces/", this::handleTraceByEventId);
        httpServer.createContext("/api/questions", this::handleQuestion);
    }

    private void handleIndexPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlainText(exchange, 405, "Method not allowed");
            return;
        }

        byte[] htmlBytes = readResource("/ui/index.html").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, htmlBytes.length);
        exchange.getResponseBody().write(htmlBytes);
        exchange.close();
    }

    private void handleMeta(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlainText(exchange, 405, "Method not allowed");
            return;
        }

        writeJson(exchange, 200, Map.of(
                "applicationMode", config.applicationMode().name().toLowerCase(),
                "llmMode", config.llmModeLabel(),
                "traceFile", traceRepository.logPath().toString()
        ));
    }

    private void handleIncidents(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 200, buildIncidentListResponse());
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, Object> requestBody = readJsonMap(exchange);

            String eventId = readText(requestBody.get("eventId"));
            if (eventId.isBlank()) {
                eventId = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            } else {
                eventId = eventId.trim().toUpperCase();
            }

            String details = readText(requestBody.get("details"));
            String analystNote = readText(requestBody.get("analystNote"));
            String system = readText(requestBody.get("system"));
            String eventType = readText(requestBody.get("eventType"));
            String severity = readText(requestBody.get("severity"));

            if (system.isBlank() || eventType.isBlank() || severity.isBlank() || details.isBlank()) {
                writePlainText(exchange, 400, "system, eventType, severity, and details are required.");
                return;
            }

            String mergedDetails = analystNote.isBlank()
                    ? details
                    : details + System.lineSeparator() + "Analyst note: " + analystNote;

            RawIncident rawIncident = new RawIncident(
                    eventId,
                    system,
                    eventType,
                    severity,
                    mergedDetails
            );

            incidentRegistry.recordSubmission(rawIncident);
            actorSystem.tell(new RootActor.SubmitRawIncident(rawIncident));

            writeJson(exchange, 202, Map.of(
                    "eventId", eventId,
                    "status", "submitted"
            ));
            return;
        }

        writePlainText(exchange, 405, "Method not allowed");
    }

    private void handleIncidentById(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlainText(exchange, 405, "Method not allowed");
            return;
        }

        String eventId = extractTrailingPath(exchange, "/api/incidents/");
        if (eventId == null || eventId.isBlank()) {
            writePlainText(exchange, 400, "Event id is required");
            return;
        }

        IncidentCaseResult result = incidentRegistry.findCompletedResult(eventId);
        if (result != null) {
            writeJson(exchange, 200, Map.of(
                    "status", "completed",
                    "result", result
            ));
            return;
        }

        RawIncident submittedIncident = incidentRegistry.findSubmittedIncident(eventId);
        if (submittedIncident != null) {
            writeJson(exchange, 200, Map.of(
                    "status", "processing",
                    "incident", submittedIncident
            ));
            return;
        }

        writePlainText(exchange, 404, "Incident not found");
    }

    private void handleTraceByEventId(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlainText(exchange, 405, "Method not allowed");
            return;
        }

        String eventId = extractTrailingPath(exchange, "/api/traces/");
        if (eventId == null || eventId.isBlank()) {
            writePlainText(exchange, 400, "Event id is required");
            return;
        }

        List<AuditEvent> traceEvents = traceRepository.findByEventId(eventId);
        writeJson(exchange, 200, traceEvents);
    }

    private void handleQuestion(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writePlainText(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> requestBody = readJsonMap(exchange);
        String eventId = readText(requestBody.get("eventId"));
        String question = readText(requestBody.get("question"));

        IncidentCaseResult result = incidentRegistry.findCompletedResult(eventId);
        if (result == null) {
            writePlainText(exchange, 404, "Incident is not completed yet.");
            return;
        }

        String answer = incidentQuestionService.answerQuestion(
                result,
                traceRepository.findByEventId(eventId),
                question
        );

        writeJson(exchange, 200, new QuestionAnswer(
                eventId,
                question,
                answer,
                config.llmModeLabel()
        ));
    }

    private List<Map<String, Object>> buildIncidentListResponse() {
        return incidentRegistry.findAllKnownEventIds().stream()
                .map(this::buildIncidentSummary)
                .toList();
    }

    private Map<String, Object> buildIncidentSummary(String eventId) {
        IncidentCaseResult result = incidentRegistry.findCompletedResult(eventId);
        RawIncident submitted = incidentRegistry.findSubmittedIncident(eventId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("eventId", eventId);
        summary.put("status", result != null ? "completed" : "processing");
        summary.put("system", result != null ? result.incident().system() : safeSubmittedField(submitted, RawIncident::system));
        summary.put("eventType", result != null ? result.incident().eventType() : safeSubmittedField(submitted, RawIncident::eventType));
        summary.put("inputSeverity", result != null ? result.incident().severity().name() : safeSubmittedField(submitted, RawIncident::severity));
        summary.put("details", result != null ? result.incident().details() : safeSubmittedField(submitted, RawIncident::details));

        if (result != null) {
            summary.put("finalSeverity", result.finalDecision().finalSeverity().name());
            summary.put("finalAction", result.finalDecision().finalAction().name());
            summary.put("escalationTarget", result.escalationResult().target().displayName());
            summary.put("llmAccepted", result.finalDecision().llmAccepted());
        }

        return summary;
    }

    private Map<String, Object> readJsonMap(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return OBJECT_MAPPER.readValue(inputStream, OBJECT_MAPPER.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, String.class, Object.class));
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] jsonBytes = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(body)
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, jsonBytes.length);
        exchange.getResponseBody().write(jsonBytes);
        exchange.close();
    }

    private void writePlainText(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bodyBytes.length);
        exchange.getResponseBody().write(bodyBytes);
        exchange.close();
    }

    private String extractTrailingPath(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith(prefix)) {
            return null;
        }
        return path.substring(prefix.length());
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = LocalUiServer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing UI resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String safeSubmittedField(RawIncident rawIncident, java.util.function.Function<RawIncident, String> reader) {
        return rawIncident == null ? "" : reader.apply(rawIncident);
    }
}
