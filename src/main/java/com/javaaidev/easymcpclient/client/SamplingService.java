package com.javaaidev.easymcpclient.client;

import io.modelcontextprotocol.spec.McpSchema.BlobResourceContents;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult.StopReason;
import io.modelcontextprotocol.spec.McpSchema.EmbeddedResource;
import io.modelcontextprotocol.spec.McpSchema.ImageContent;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;

public class SamplingService implements Function<CreateMessageRequest, CreateMessageResult> {

  private final ChatClient chatClient;

  public SamplingService(ChatModel chatModel) {
    chatClient = ChatClient.builder(chatModel).build();
  }

  @Override
  public CreateMessageResult apply(CreateMessageRequest request) {
    var optionsBuilder = ChatOptions.builder();
    if (request.temperature() != null) {
      optionsBuilder.temperature(request.temperature());
    }
    if (request.maxTokens() > 0) {
      optionsBuilder.maxTokens(request.maxTokens());
    }
    if (!CollectionUtils.isEmpty(request.stopSequences())) {
      optionsBuilder.stopSequences(request.stopSequences());
    }
    var messages = request.messages().stream().map(this::fromSamplingMessage).toList();
    var spec = chatClient.prompt().messages(messages)
        .options(optionsBuilder.build());
    if (StringUtils.isNotEmpty(request.systemPrompt())) {
      spec.system(request.systemPrompt());
    }
    var chatResponse = spec.call().chatResponse();
    if (chatResponse == null) {
      throw new SamplingException("No response from LLM");
    }
    var model = Optional.ofNullable(chatResponse.getMetadata()).map(ChatResponseMetadata::getModel)
        .orElse("");
    var resultContent = chatResponse.getResult().getOutput().getText();
    return new CreateMessageResult(Role.ASSISTANT, new TextContent(resultContent), model,
        StopReason.END_TURN);
  }

  private Message fromSamplingMessage(SamplingMessage message) {
    var content = message.content();
    String text = null;
    Media media = null;
    if (content instanceof TextContent textContent) {
      text = textContent.text();
    } else if (content instanceof ImageContent imageContent) {
      media = Media.builder()
          .data(imageContent.data())
          .mimeType(MimeTypeUtils.parseMimeType(imageContent.mimeType()))
          .build();
    } else if (content instanceof EmbeddedResource embeddedResource) {
      var resource = embeddedResource.resource();
      if (resource instanceof TextResourceContents textResourceContents) {
        text = textResourceContents.text();
      } else if (resource instanceof BlobResourceContents blobResourceContents) {
        media = Media.builder()
            .data(blobResourceContents.blob())
            .mimeType(MimeTypeUtils.parseMimeType(blobResourceContents.mimeType()))
            .build();
      }
    }
    return switch (message.role()) {
      case USER -> {
        var userMessageBuilder = UserMessage.builder();
        if (text != null) {
          userMessageBuilder.text(text);
        }
        if (media != null) {
          userMessageBuilder.media(media);
        }
        yield userMessageBuilder.build();
      }
      case ASSISTANT -> {
        var mediaList = media != null ? List.of(media) : List.<Media>of();
        yield AssistantMessage.builder()
            .content(Objects.requireNonNullElse(text, ""))
            .media(mediaList).build();
      }
    };
  }
}
