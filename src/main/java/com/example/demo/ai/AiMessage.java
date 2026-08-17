package com.example.demo.ai;

/** One turn in a chat-completion request. {@code role} is "system", "user", or "assistant". */
public record AiMessage(String role, String content) {

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }

    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }
}
