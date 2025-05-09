package com.javaaidev.easymcpclient.config.mcp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SseServer.class, name = "sse"),
    @JsonSubTypes.Type(value = StdioServer.class, name = "stdio")})
public sealed interface McpServer permits SseServer, StdioServer {

  default String type() {
    if (this instanceof SseServer) {
      return "sse";
    } else if (this instanceof StdioServer) {
      return "stdio";
    }
    throw new IllegalArgumentException("Unknown server type: " + this);
  }
}
