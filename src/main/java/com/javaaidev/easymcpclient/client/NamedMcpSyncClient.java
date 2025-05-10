package com.javaaidev.easymcpclient.client;

import io.modelcontextprotocol.client.McpSyncClient;

public record NamedMcpSyncClient(String name, McpSyncClient mcpSyncClient) {

}
