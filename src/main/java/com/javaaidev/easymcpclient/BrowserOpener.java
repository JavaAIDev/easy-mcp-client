package com.javaaidev.easymcpclient;

import java.io.IOException;
import java.util.List;

public class BrowserOpener {

  public static void openUrl(String url) throws IOException {
    var os = System.getProperty("os.name").toLowerCase();
    String[] commands;
    if (os.contains("win")) {
      commands = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
    } else if (os.contains("mac")) {
      commands = new String[]{"open", url};
    } else {
      var browsers = List.of("google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
          "netscape", "opera", "links", "lynx");
      var command = String.join(" || ",
          browsers.stream().map(browser -> "%s \"%s\"".formatted(browser, url)).toList());
      commands = new String[]{"sh", "-c", command};
    }
    Runtime.getRuntime().exec(commands);
  }
}
