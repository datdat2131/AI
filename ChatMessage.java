package com.astrax.ai.api;

/** Một tin nhắn trong hội thoại. role là "user" hoặc "assistant". */
public record ChatMessage(String role, String content) {
}
