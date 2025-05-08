package com.javaaidev.easymcpclient.config;

import com.javaaidev.easymcpclient.config.chatmodel.ChatModelConfig;
import com.javaaidev.easymcpclient.config.mcp.McpServer;
import java.util.List;

public record McpClientConfig(List<McpServer> servers, ChatModelConfig chatModel) {

}
