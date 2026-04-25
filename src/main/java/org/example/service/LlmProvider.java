package org.example.service;

import org.example.model.IncidentContext;
import org.example.model.LlmAnalysis;

/**
 * Simple interface so the project can switch between mock and OpenAI modes.
 */
public interface LlmProvider {
    LlmAnalysis analyze(IncidentContext context);
}
