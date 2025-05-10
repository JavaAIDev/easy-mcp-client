package com.javaaidev.easymcpclient.client;

import java.util.List;

public record CloseableMcpSyncClients(List<NamedMcpSyncClient> clients) implements AutoCloseable {

  @Override
  public void close() {
    this.clients.forEach(client -> client.mcpSyncClient().close());
  }
}