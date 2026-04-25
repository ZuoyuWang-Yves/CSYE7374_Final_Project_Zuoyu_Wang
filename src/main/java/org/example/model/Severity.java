package org.example.model;

/**
 * Lists the severity levels used in the project.
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static Severity fromInput(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Severity is required");
        }
        return Severity.valueOf(value.trim().toUpperCase());
    }
}
