package org.example.service;

import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;

public interface LlmProvider {
    LlmAnalysis analyze(IncidentContext context);
}
