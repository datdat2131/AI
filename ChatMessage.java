package com.astrax.ai;

/** Một tin nhắn trong hội thoại. role là "user" hoặc "assistant". */
public record ChatMessage(String role, String content) {
}
