package com.javaaidev.easymcpclient.client;

import com.javaaidev.easymcpclient.config.mcp.McpServer;
import com.javaaidev.easymcpclient.config.mcp.SseServer;
import com.javaaidev.easymcpclient.config.mcp.StdioServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

public class McpClientService {

  public List<McpSyncClient> connect(Collection<McpServer> servers) {
    return servers.stream().map(server -> {
      if (server instanceof StdioServer stdioServer) {
        return connect(stdioServer);
      } else if (server instanceof SseServer sseServer) {
        return connect(sseServer);
      }
      throw new IllegalArgumentException("Invalid MCP server");
    }).toList();
  }

  private McpSyncClient connect(SseServer server) {
    return doConnect(HttpClientSseClientTransport.builder(server.url()).build());
  }

  private McpSyncClient connect(StdioServer server) {
    return doConnect(new StdioClientTransport(
        ServerParameters.builder(server.command())
            .args(server.args())
            .env(server.env())
            .build()));
  }

  private McpSyncClient doConnect(McpClientTransport clientTransport) {
    var client = McpClient.sync(clientTransport)
        .clientInfo(new Implementation("easy-mcp-client", "0.1.0"))
        .requestTimeout(Duration.ofSeconds(30))
        .capabilities(ClientCapabilities.builder()
            .sampling()
            .build())
        .build();
    client.initialize();
    return client;
  }
}
