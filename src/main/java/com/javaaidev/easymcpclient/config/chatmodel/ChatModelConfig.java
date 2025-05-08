package com.javaaidev.easymcpclient.config.chatmodel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OpenAIChatModelConfig.class, name = "openai")})
public sealed interface ChatModelConfig permits OpenAIChatModelConfig {

  default String type() {
    if (this instanceof OpenAIChatModelConfig) {
      return "openai";
    }
    throw new IllegalArgumentException("Unknown server type: " + this);
  }
}
