package com.example.demo.ai;

/** {@code promptTokens}/{@code completionTokens} are null if the gateway didn't report usage. */
public record AiCompletionResponse(String content, String finishReason, Integer promptTokens, Integer completionTokens) {
}
