package com.javaaidev.easymcpclient.config.chatmodel;

public record OpenAIChatModelConfig(
    String baseUrl,
    String apiKey,
    String apiKeyEnv,
    String model,
    Double temperature) implements ChatModelConfig {

}
