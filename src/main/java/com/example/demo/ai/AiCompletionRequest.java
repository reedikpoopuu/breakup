package com.example.demo.ai;

import java.util.List;

/** {@code maxTokens}/{@code temperature} may be null - the client applies its own defaults. */
public record AiCompletionRequest(List<AiMessage> messages, Integer maxTokens, Double temperature) {

    public static AiCompletionRequest of(List<AiMessage> messages) {
        return new AiCompletionRequest(messages, null, null);
    }
}
