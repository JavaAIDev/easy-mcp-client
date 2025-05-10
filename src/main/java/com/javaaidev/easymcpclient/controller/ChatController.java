package com.javaaidev.easymcpclient.controller;

import com.javaaidev.chatagent.model.ChatAgentRequest;
import com.javaaidev.chatagent.model.ChatAgentResponse;
import com.javaaidev.chatagent.springai.ModelAdapter;
import com.javaaidev.easymcpclient.client.McpToolCallbackResolver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

  private final ChatClient chatClient;
  private final McpToolCallbackResolver mcpToolCallbackResolver;

  public ChatController(ChatModel chatModel, McpToolCallbackResolver mcpToolCallbackResolver) {
    this.mcpToolCallbackResolver = mcpToolCallbackResolver;
    this.chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new SimpleLoggerAdvisor())
        .build();
  }

  @PostMapping("/chat")
  public Flux<ServerSentEvent<ChatAgentResponse>> chat(@RequestBody ChatAgentRequest request) {
    return ModelAdapter.toStreamingResponse(
        chatClient.prompt()
            .toolNames(mcpToolCallbackResolver.getToolNames())
            .messages(ModelAdapter.fromRequest(request).toArray(new Message[0]))
            .stream()
            .chatResponse());
  }
}
