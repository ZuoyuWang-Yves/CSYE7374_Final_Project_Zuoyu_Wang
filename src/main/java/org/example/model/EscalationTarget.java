package org.example.model;

public enum EscalationTarget {
    NURSE("Nurse"),
    GENERAL_DOCTOR("General Doctor"),
    SPECIALIST("Specialist"),
    SENIOR_DOCTOR("Senior Doctor");

    private final String displayName;

    EscalationTarget(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
