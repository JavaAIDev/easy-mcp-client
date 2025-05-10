package com.javaaidev.easymcpclient.client;

import org.springframework.context.ApplicationEvent;

public class ToolsChangedEvent extends ApplicationEvent {

  public ToolsChangedEvent(String clientName) {
    super(clientName);
  }

  public String getClientName() {
    return (String) this.getSource();
  }
}
