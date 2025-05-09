package com.javaaidev.easymcpclient.config.mcp;

import java.util.List;
import java.util.Map;

public record StdioServer(String command,
                          List<String> args,
                          Map<String, String> env) implements McpServer {

}
