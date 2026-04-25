package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.AuditEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraceRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final ZoneId EASTERN_TIME_ZONE = ZoneId.of("America/New_York");

    private final Path logPath;

    public TraceRepository(Path logPath) {
        this.logPath = logPath;
    }

    public Path logPath() {
        return logPath;
    }

    public List<AuditEvent> findAll() {
        List<AuditEvent> events = new ArrayList<>();

        if (!Files.exists(logPath)) {
            return events;
        }

        try {
            for (String line : Files.readAllLines(logPath)) {
                if (line.isBlank()) {
                    continue;
                }

                AuditEvent event = parseLine(line);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read trace log from " + logPath, exception);
        }

        events.sort(Comparator.comparing(AuditEvent::timestamp));
        return events;
    }

    public List<AuditEvent> findByEventId(String eventId) {
        List<AuditEvent> matchingEvents = new ArrayList<>();

        for (AuditEvent event : findAll()) {
            if (event.eventId().equals(eventId)) {
                matchingEvents.add(event);
            }
        }

        matchingEvents.sort(Comparator.comparing(AuditEvent::timestamp));
        return matchingEvents;
    }

    private AuditEvent parseLine(String line) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(line);

            Instant timestamp = Instant.parse(node.path("timestamp").asText());
            ZonedDateTime timestampEastern = node.hasNonNull("timestampEastern")
                    ? ZonedDateTime.parse(node.path("timestampEastern").asText())
                    : timestamp.atZone(EASTERN_TIME_ZONE);

            Map<String, Object> details = OBJECT_MAPPER.convertValue(
                    node.path("details"),
                    OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );

            return new AuditEvent(
                    timestamp,
                    timestampEastern,
                    node.path("eventId").asText(),
                    node.path("actorName").asText(),
                    node.path("action").asText(),
                    details
            );
        } catch (Exception exception) {
            return null;
        }
    }
}
