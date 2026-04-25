package org.example.service;

import org.example.model.NormalizedIncident;
import org.example.model.RiskResult;
import org.example.model.Severity;

public class RiskRules {

    public RiskResult analyze(NormalizedIncident incident) {
        String eventType = incident.eventType();
        Severity inputSeverity = incident.severity();

        if ("oxygen_drop".equals(eventType) && inputSeverity == Severity.HIGH) {
            return new RiskResult(
                    incident.eventId(),
                    Severity.CRITICAL,
                    "Oxygen drop with high severity is treated as critical."
            );
        }

        if ("dose_sync_failure".equals(eventType) && inputSeverity.ordinal() >= Severity.MEDIUM.ordinal()) {
            return new RiskResult(
                    incident.eventId(),
                    Severity.HIGH,
                    "Dose synchronization failure at medium or higher severity is high risk."
            );
        }

        if ("sensor_dropout".equals(eventType) && inputSeverity == Severity.HIGH) {
            return new RiskResult(
                    incident.eventId(),
                    Severity.HIGH,
                    "High-severity sensor dropout requires specialist review."
            );
        }

        return new RiskResult(
                incident.eventId(),
                inputSeverity,
                "No special risk escalation rule matched."
        );
    }
}
