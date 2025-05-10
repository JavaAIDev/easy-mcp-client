package com.javaaidev.easymcpclient.client;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

public class McpToolCallbackResolver implements ToolCallbackResolver,
    ApplicationListener<ToolsChangedEvent> {

  private final Map<String, McpSyncClient> mcpClients;
  private final Map<String, Map<String, ToolCallback>> toolCallbacks = new HashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(McpToolCallbackResolver.class);

  public McpToolCallbackResolver(List<NamedMcpSyncClient> mcpClients) {
    this.mcpClients = mcpClients.stream()
        .collect(Collectors.toMap(NamedMcpSyncClient::name, NamedMcpSyncClient::mcpSyncClient));
    refresh();
  }

  @Override
  public ToolCallback resolve(String toolName) {
    Assert.hasText(toolName, "toolName cannot be null or empty");
    for (Map<String, ToolCallback> callbackMap : toolCallbacks.values()) {
      var callback = callbackMap.get(toolName);
      if (callback != null) {
        return callback;
      }
    }
    return null;
  }

  public synchronized String[] getToolNames() {
    return toolCallbacks.values().stream()
        .flatMap(map -> map.keySet().stream()).toList().toArray(String[]::new);
  }

  private synchronized void refresh() {
    mcpClients.keySet().forEach(this::refreshTools);
  }

  @Override
  public void onApplicationEvent(ToolsChangedEvent event) {
    refreshTools(event.getClientName());
  }

  private synchronized void refreshTools(String name) {
    var mcpClient = mcpClients.get(name);
    if (mcpClient == null) {
      return;
    }
    var tools = mcpClient.listTools()
        .tools()
        .stream()
        .map(tool -> new SyncMcpToolCallback(mcpClient, tool))
        .collect(Collectors.toMap(toolCallback -> toolCallback.getToolDefinition().name(),
            Function.<ToolCallback>identity()));
    toolCallbacks.put(name, tools);
    LOGGER.info("Refreshed tools for {}, found tools {}", name, tools.keySet());
  }
}
