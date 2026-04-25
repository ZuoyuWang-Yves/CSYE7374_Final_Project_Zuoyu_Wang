package org.example.model;

/**
 * Stores one question asked from the UI and the returned answer.
 */
public record QuestionAnswer(
        String eventId,
        String question,
        String answer,
        String llmMode
) {
}
