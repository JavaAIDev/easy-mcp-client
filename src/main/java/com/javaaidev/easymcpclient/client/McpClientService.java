package com.javaaidev.easymcpclient.client;

import com.javaaidev.easymcpclient.config.mcp.NamedMcpServer;
import com.javaaidev.easymcpclient.config.mcp.SseServer;
import com.javaaidev.easymcpclient.config.mcp.StdioServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

public class McpClientService {

  private static final Logger LOGGER = LoggerFactory.getLogger(McpClientService.class);

  private final SamplingService samplingService;
  private final ApplicationEventPublisher applicationEventPublisher;

  public McpClientService(SamplingService samplingService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.samplingService = samplingService;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public List<NamedMcpSyncClient> connect(Collection<NamedMcpServer> servers) {
    return servers.stream().map(namedMcpServer -> {
      var name = namedMcpServer.name();
      var server = namedMcpServer.mcpServer();
      if (server instanceof StdioServer stdioServer) {
        return connect(name, stdioServer);
      } else if (server instanceof SseServer sseServer) {
        return connect(name, sseServer);
      }
      return Optional.<NamedMcpSyncClient>empty();
    }).flatMap(Optional::stream).toList();
  }

  private Optional<NamedMcpSyncClient> connect(String name, SseServer server) {
    return doConnect(name, HttpClientSseClientTransport.builder(server.url()).build());
  }

  private Optional<NamedMcpSyncClient> connect(String name, StdioServer server) {
    return doConnect(name, new StdioClientTransport(
        ServerParameters.builder(server.command())
            .args(server.args())
            .env(server.env())
            .build()));
  }

  private Optional<NamedMcpSyncClient> doConnect(String name, McpClientTransport clientTransport) {
    try {
      var client = McpClient.sync(clientTransport)
          .clientInfo(new Implementation("easy-mcp-client", "0.1.0"))
          .requestTimeout(Duration.ofSeconds(30))
          .initializationTimeout(Duration.ofSeconds(30))
          .capabilities(ClientCapabilities.builder()
              .sampling()
              .build())
          .sampling(samplingService)
          .toolsChangeConsumer(
              tools -> applicationEventPublisher.publishEvent(new ToolsChangedEvent(name)))
          .build();
      client.initialize();
      return Optional.of(new NamedMcpSyncClient(name, client));
    } catch (Exception e) {
      LOGGER.error("Failed to connect to MCP server {}", name, e);
    }
    return Optional.empty();
  }
}
