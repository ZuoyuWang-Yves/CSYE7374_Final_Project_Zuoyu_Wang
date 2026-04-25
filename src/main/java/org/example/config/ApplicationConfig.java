package org.example.config;

public record ApplicationConfig(
        ApplicationMode applicationMode,
        LlmMode llmMode,
        int uiPort,
        String openAiApiKey,
        String openAiModel,
        String openAiResponsesEndpoint
) {

    public enum ApplicationMode {
        DEMO,
        SERVER
    }

    public enum LlmMode {
        MOCK,
        OPENAI
    }

    public static ApplicationConfig fromEnvironment(String[] args) {
        String appModeValue = readModeFlag(args, "--demo", "--server", "APP_MODE", "server");
        String llmModeValue = readModeFlag(args, "--mock", "--openai", "LLM_MODE", "mock");
        int uiPort = Integer.parseInt(readValueFlag(args, "--port=", "UI_PORT", "8080"));

        ApplicationMode applicationMode = "demo".equalsIgnoreCase(appModeValue)
                ? ApplicationMode.DEMO
                : ApplicationMode.SERVER;
        LlmMode llmMode = "openai".equalsIgnoreCase(llmModeValue)
                ? LlmMode.OPENAI
                : LlmMode.MOCK;

        return new ApplicationConfig(
                applicationMode,
                llmMode,
                uiPort,
                System.getenv("OPENAI_API_KEY"),
                System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5.1"),
                System.getenv().getOrDefault("OPENAI_RESPONSES_ENDPOINT", "https://api.openai.com/v1/responses")
        );
    }

    public static ApplicationConfig demoDefaults() {
        return new ApplicationConfig(
                ApplicationMode.DEMO,
                LlmMode.MOCK,
                8080,
                null,
                "gpt-5.1",
                "https://api.openai.com/v1/responses"
        );
    }

    public boolean runDemoBatch() {
        return applicationMode == ApplicationMode.DEMO;
    }

    public boolean runUiServer() {
        return applicationMode == ApplicationMode.SERVER;
    }

    public String llmModeLabel() {
        return llmMode == LlmMode.OPENAI ? "openai" : "mock";
    }

    private static String readModeFlag(
            String[] args,
            String firstFlag,
            String secondFlag,
            String envName,
            String defaultValue
    ) {
        for (String arg : args) {
            if (firstFlag.equals(arg)) {
                return normalizeFlag(firstFlag);
            }
            if (secondFlag.equals(arg)) {
                return normalizeFlag(secondFlag);
            }
        }

        return System.getenv().getOrDefault(envName, defaultValue);
    }

    private static String readValueFlag(
            String[] args,
            String prefix,
            String envName,
            String defaultValue
    ) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }

        return System.getenv().getOrDefault(envName, defaultValue);
    }

    private static String normalizeFlag(String flag) {
        return flag.replace("--", "");
    }
}
