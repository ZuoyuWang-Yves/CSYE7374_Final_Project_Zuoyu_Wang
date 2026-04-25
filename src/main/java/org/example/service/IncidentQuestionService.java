package org.example.service;

import org.example.config.ApplicationConfig;
import org.example.model.AuditEvent;
import org.example.model.IncidentCaseResult;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Answers follow-up questions about one completed incident.
 */
public class IncidentQuestionService {

    private final ApplicationConfig config;
    private final OpenAiResponsesClient openAiClient;

    public IncidentQuestionService(ApplicationConfig config) {
        this.config = config;
        this.openAiClient = config.llmMode() == ApplicationConfig.LlmMode.OPENAI
                && config.openAiApiKey() != null
                && !config.openAiApiKey().isBlank()
                ? new OpenAiResponsesClient(
                config.openAiApiKey(),
                config.openAiModel(),
                config.openAiResponsesEndpoint()
        )
                : null;
    }

    public String answerQuestion(IncidentCaseResult result, List<AuditEvent> traceEvents, String question) {
        if (openAiClient != null) {
            try {
                return openAiClient.createTextResponse(
                        """
                                You are answering questions about a simulated medical software incident triage run.
                                Use the supplied case result and trace details only.
                                Be concise and precise.
                                """.strip(),
                        buildQuestionPrompt(result, traceEvents, question),
                        300
                );
            } catch (Exception exception) {
                return "OpenAI question answering failed, so a local summary was used instead. "
                        + buildMockAnswer(result, traceEvents, question);
            }
        }

        return buildMockAnswer(result, traceEvents, question);
    }

    private String buildQuestionPrompt(
            IncidentCaseResult result,
            List<AuditEvent> traceEvents,
            String question
    ) {
        StringBuilder traceBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        for (AuditEvent event : traceEvents) {
            traceBuilder.append("- ")
                    .append(event.timestampEastern().format(formatter))
                    .append(" | ")
                    .append(event.actorName())
                    .append(" | ")
                    .append(event.action())
                    .append(" | ")
                    .append(event.details())
                    .append(System.lineSeparator());
        }

        return """
                Incident:
                %s

                Final decision:
                severity=%s, action=%s, reason=%s

                LLM analysis:
                analysis=%s
                recommendation=%s

                Trace:
                %s

                User question:
                %s
                """.formatted(
                result.incident(),
                result.finalDecision().finalSeverity(),
                result.finalDecision().finalAction(),
                result.finalDecision().reason(),
                result.llmAnalysis().analysis(),
                result.llmAnalysis().recommendedAction(),
                traceBuilder,
                question
        );
    }

    private String buildMockAnswer(
            IncidentCaseResult result,
            List<AuditEvent> traceEvents,
            String question
    ) {
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("why")) {
            return result.finalDecision().reason();
        }

        if (lowerQuestion.contains("policy")) {
            return "Applied policies: " + result.policyResult().appliedPolicies();
        }

        if (lowerQuestion.contains("llm")) {
            return "LLM analysis: " + result.llmAnalysis().analysis()
                    + " Recommended action: " + result.llmAnalysis().recommendedAction();
        }

        return "Final severity was " + result.finalDecision().finalSeverity()
                + ", final action was " + result.finalDecision().finalAction()
                + ", escalation target was " + result.escalationResult().target().displayName()
                + ". Trace contained " + traceEvents.size() + " recorded events.";
    }
}
