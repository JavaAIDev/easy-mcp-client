package com.javaaidev.easymcpclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.javaaidev.easymcpclient.chatmodel.ChatModelService;
import com.javaaidev.easymcpclient.client.McpClientService;
import com.javaaidev.easymcpclient.config.McpClientConfig;
import io.modelcontextprotocol.client.McpSyncClient;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .addModules(JacksonUtils.instantiateAvailableModules())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }

  @Bean
  public McpClientConfig mcpClientConfig(ApplicationArguments arguments,
      ObjectMapper objectMapper) {
    var args = arguments.getNonOptionArgs();
    if (args.isEmpty()) {
      throw new RuntimeException("Config file is required");
    }
    var configFile = Path.of(args.get(0)).toFile();
    try {
      return objectMapper.readValue(configFile, McpClientConfig.class);
    } catch (IOException e) {
      throw new RuntimeException("Invalid config file", e);
    }
  }

  @Bean
  public ChatModel chatModel(McpClientConfig mcpClientConfig) {
    return new ChatModelService().create(mcpClientConfig.chatModel());
  }

  @Bean
  public List<McpSyncClient> mcpSyncClients(McpClientConfig mcpClientConfig) {
    return new McpClientService().connect(mcpClientConfig.mcpServers().values());
  }

  @Bean
  public ToolCallbackProvider mcpToolCallbacks(ObjectProvider<List<McpSyncClient>> syncMcpClients) {
    List<McpSyncClient> mcpClients = syncMcpClients.stream().flatMap(List::stream).toList();
    return new SyncMcpToolCallbackProvider(mcpClients);
  }
}
