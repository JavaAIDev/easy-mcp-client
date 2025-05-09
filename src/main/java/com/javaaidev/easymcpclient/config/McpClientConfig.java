package com.javaaidev.easymcpclient.config;

import com.javaaidev.easymcpclient.config.chatmodel.ChatModelConfig;
import com.javaaidev.easymcpclient.config.mcp.McpServer;
import java.util.Map;

public record McpClientConfig(Map<String, McpServer> mcpServers, ChatModelConfig chatModel) {

}
