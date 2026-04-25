package org.example.model;

public record QuestionAnswer(
        String eventId,
        String question,
        String answer,
        String llmMode
) {
}
