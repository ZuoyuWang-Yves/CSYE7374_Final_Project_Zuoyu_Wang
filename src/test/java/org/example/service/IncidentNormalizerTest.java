package org.example.service;

import org.example.model.NormalizedIncident;
import org.example.model.RawIncident;
import org.example.model.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncidentNormalizerTest {

    private final IncidentNormalizer normalizer = new IncidentNormalizer();

    @Test
    void normalizeGeneratesEventIdWhenMissing() {
        RawIncident rawIncident = new RawIncident(
                null,
                "VitalsMonitor",
                "oxygen_drop",
                "high",
                "SpO2 < 88% for 40s"
        );

        NormalizedIncident normalized = normalizer.normalize(rawIncident);

        assertFalse(normalized.eventId().isBlank());
        assertEquals("VitalsMonitor", normalized.system());
        assertEquals("oxygen_drop", normalized.eventType());
        assertEquals(Severity.HIGH, normalized.severity());
    }

    @Test
    void normalizeRejectsMissingSystem() {
        RawIncident rawIncident = new RawIncident(
                "EVT-1001",
                " ",
                "oxygen_drop",
                "high",
                "SpO2 < 88% for 40s"
        );

        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(rawIncident));
    }
}
