package com.LogicProjector.analysis;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface AiChatClient {

    JsonNode createStructuredResponse(String systemPrompt, String userPrompt);
}
