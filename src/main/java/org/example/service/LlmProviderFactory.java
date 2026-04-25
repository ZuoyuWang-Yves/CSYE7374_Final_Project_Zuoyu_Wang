package org.example.service;

import org.example.config.ApplicationConfig;

public final class LlmProviderFactory {

    private LlmProviderFactory() {
    }

    public static LlmProvider createProvider(ApplicationConfig config) {
        if (config.llmMode() == ApplicationConfig.LlmMode.OPENAI) {
            if (config.openAiApiKey() == null || config.openAiApiKey().isBlank()) {
                throw new IllegalStateException("LLM_MODE=openai requires OPENAI_API_KEY to be set.");
            }

            return new OpenAiLlmProvider(
                    new OpenAiResponsesClient(
                            config.openAiApiKey(),
                            config.openAiModel(),
                            config.openAiResponsesEndpoint()
                    )
            );
        }

        return new MockLlmProvider();
    }
}
