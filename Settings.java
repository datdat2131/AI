package com.astrax.ai.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Bản chụp bất biến của config.yml. Mỗi lần reload sẽ tạo một Settings mới.
 * API Key chỉ nằm trong đối tượng này và không bao giờ được ghi log.
 */
public final class Settings {

    public final String apiKey;
    public final String apiUrl;
    public final String apiFormat;
    public final String anthropicVersion;
    public final String model;
    public final int maxTokens;
    public final int adminMaxTokens;
    public final int timeoutSeconds;

    public final boolean chatEnabled;
    public final int cooldownSeconds;
    public final int maxMessageLength;
    public final int inputTimeoutSeconds;

    public final boolean memoryEnabled;
    public final int maxHistory;
    public final boolean clearOnQuit;

    public final boolean adminEnabled;
    public final int adminCooldownSeconds;

    public final int threads;
    public final int knowledgeMaxChars;

    public Settings(FileConfiguration c) {
        String key = trim(c.getString("api.key", ""));
        if (key.isEmpty()) {
            key = trim(System.getenv("ASTRAXAI_API_KEY"));
        }
        this.apiKey = key;
        this.apiUrl = trim(c.getString("api.url", "https://api.anthropic.com/v1/messages"));

        String format = trim(c.getString("api.format", "anthropic")).toLowerCase(Locale.ROOT);
        this.apiFormat = format.equals("openai") ? "openai" : "anthropic";

        String version = trim(c.getString("api.anthropic-version", "2023-06-01"));
        this.anthropicVersion = version.isEmpty() ? "2023-06-01" : version;

        String model = trim(c.getString("api.model", "claude-haiku-5-5"));
        this.model = model.isEmpty() ? "claude-haiku-5-5" : model;

        this.maxTokens = clamp(c.getInt("api.max-tokens", 500), 50, 4000);
        this.adminMaxTokens = clamp(c.getInt("api.admin-max-tokens", 1000), 100, 4000);
        this.timeoutSeconds = clamp(c.getInt("api.timeout-seconds", 30), 5, 120);

        this.chatEnabled = c.getBoolean("chat.enabled", true);
        this.cooldownSeconds = clamp(c.getInt("chat.cooldown-seconds", 5), 0, 3600);
        this.maxMessageLength = clamp(c.getInt("chat.max-message-length", 300), 20, 2000);
        this.inputTimeoutSeconds = clamp(c.getInt("chat.input-timeout-seconds", 30), 5, 300);

        this.memoryEnabled = c.getBoolean("memory.enabled", true);
        this.maxHistory = clamp(c.getInt("memory.max-history", 10), 2, 50);
        this.clearOnQuit = c.getBoolean("memory.clear-on-quit", true);

        this.adminEnabled = c.getBoolean("admin.enabled", true);
        this.adminCooldownSeconds = clamp(c.getInt("admin.cooldown-seconds", 30), 0, 3600);

        this.threads = clamp(c.getInt("advanced.threads", 2), 1, 8);
        this.knowledgeMaxChars = clamp(c.getInt("advanced.knowledge-max-chars", 12000), 1000, 100000);
    }

    public boolean hasApiKey() {
        return !apiKey.isEmpty();
    }

    public boolean isUrlSecure() {
        return apiUrl.toLowerCase(Locale.ROOT).startsWith("https://");
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
