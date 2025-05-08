package com.javaaidev.easymcpclient.chatmodel;

import com.javaaidev.easymcpclient.config.chatmodel.ChatModelConfig;
import com.javaaidev.easymcpclient.config.chatmodel.OpenAIChatModelConfig;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class ChatModelService {

  private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();

  public ChatModel create(ChatModelConfig config) {
    if (config instanceof OpenAIChatModelConfig openAIChatModelConfig) {
      return create(openAIChatModelConfig);
    }
    throw new IllegalArgumentException("Invalid chat model config");
  }

  private ChatModel create(OpenAIChatModelConfig config) {
    String apiKey = "";
    if (StringUtils.isNotEmpty(config.apiKey())) {
      apiKey = config.apiKey();
    } else if (StringUtils.isNotEmpty(config.apiKeyEnv())) {
      apiKey = dotenv.get(config.apiKeyEnv());
    }
    if (StringUtils.isEmpty(apiKey)) {
      throw new RuntimeException("API key is required");
    }
    var openAiApiBuilder = OpenAiApi.builder()
        .apiKey(apiKey);
    safeSet(config::baseUrl, openAiApiBuilder::baseUrl);
    var chatOptionsBuilder = OpenAiChatOptions.builder();
    safeSet(config::model, chatOptionsBuilder::model);
    if (config.temperature() != null) {
      chatOptionsBuilder.temperature(config.temperature());
    }
    return OpenAiChatModel.builder()
        .openAiApi(openAiApiBuilder.build())
        .defaultOptions(chatOptionsBuilder.build())
        .build();
  }

  private void safeSet(Supplier<String> supplier, Consumer<String> consumer) {
    var value = StringUtils.trimToEmpty(supplier.get());
    if (StringUtils.isNotEmpty(value)) {
      consumer.accept(value);
    }
  }
}
