package com.javaaidev.easymcpclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.javaaidev.easymcpclient.chatmodel.ChatModelService;
import com.javaaidev.easymcpclient.client.CloseableMcpSyncClients;
import com.javaaidev.easymcpclient.client.McpClientService;
import com.javaaidev.easymcpclient.client.McpToolCallbackResolver;
import com.javaaidev.easymcpclient.client.NamedMcpSyncClient;
import com.javaaidev.easymcpclient.client.SamplingService;
import com.javaaidev.easymcpclient.config.McpClientConfig;
import com.javaaidev.easymcpclient.config.mcp.NamedMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.GenericApplicationContext;

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
  public McpJsonMapper mcpJsonMapper() {
    return McpJsonMapper.createDefault();
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
  public ChatModel chatModel(McpClientConfig mcpClientConfig,
      ToolCallingManager toolCallingManager) {
    return new ChatModelService().create(mcpClientConfig.chatModel(), toolCallingManager);
  }

  @Bean
  public SamplingService samplingService(ChatModel chatModel) {
    return new SamplingService(chatModel);
  }

  @Bean
  public McpClientService mcpClientService(@Lazy SamplingService samplingService,
      McpJsonMapper mcpJsonMapper,
      ApplicationEventPublisher applicationEventPublisher) {
    return new McpClientService(samplingService, mcpJsonMapper, applicationEventPublisher);
  }

  @Bean
  public List<NamedMcpSyncClient> mcpSyncClients(McpClientService mcpClientService,
      McpClientConfig mcpClientConfig) {
    return mcpClientService.connect(mcpClientConfig.mcpServers().entrySet().stream()
        .map(entry -> new NamedMcpServer(entry.getKey(), entry.getValue())).toList());
  }

  @Bean
  public McpToolCallbackResolver mcpToolCallbackResolver(List<NamedMcpSyncClient> syncMcpClients) {
    return new McpToolCallbackResolver(syncMcpClients);
  }

  @Bean
  public CloseableMcpSyncClients closeableMcpSyncClients(List<NamedMcpSyncClient> mcpSyncClients) {
    return new CloseableMcpSyncClients(mcpSyncClients);
  }

  @Bean
  public AppListener appListener() {
    return new AppListener();
  }

  @Bean
  public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
    return new DefaultToolExecutionExceptionProcessor(false);
  }

  @Bean
  public ToolCallingManager toolCallingManager(
      @Qualifier("mainToolCallbackResolver") ToolCallbackResolver toolCallbackResolver,
      ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
    return ToolCallingManager.builder()
        .toolCallbackResolver(toolCallbackResolver)
        .toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
        .build();
  }

  @Bean
  @Qualifier("mainToolCallbackResolver")
  public ToolCallbackResolver toolCallbackResolver(McpToolCallbackResolver mcpToolCallbackResolver,
      GenericApplicationContext applicationContext) {
    var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
        .applicationContext(applicationContext)
        .build();

    return new DelegatingToolCallbackResolver(
        List.of(mcpToolCallbackResolver, springBeanToolCallbackResolver));
  }
}
