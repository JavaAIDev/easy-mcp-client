package com.javaaidev.easymcpclient;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

public class AppListener implements
    ApplicationListener<WebServerInitializedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppListener.class);

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    int port = event.getWebServer().getPort();
    LOGGER.info("Server running on port {}", port);
    try {
      BrowserOpener.openUrl("http://localhost:%s/webjars/chat-agent-ui/index.html".formatted(port));
    } catch (IOException e) {
      LOGGER.error("Failed to open url", e);
    }
  }
}
