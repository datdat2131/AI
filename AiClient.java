package com.astrax.ai.api;

import com.astrax.ai.util.Settings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Client gọi API AI qua HTTPS, hoàn toàn bất đồng bộ (không bao giờ chặn main thread).
 * Hỗ trợ định dạng "anthropic" (Messages API) và "openai" (chat/completions).
 * API Key chỉ được gắn vào header và không bao giờ được ghi log.
 */
public final class AiClient implements AutoCloseable {

    private final Settings settings;
    private final HttpClient http;

    public AiClient(Settings settings, ExecutorService executor) {
        this.settings = settings;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(settings.timeoutSeconds, 15)))
                .followRedirects(HttpClient.Redirect.NEVER) // không để key bị chuyển sang host khác
                .executor(executor)
                .build();
    }

    public CompletableFuture<AiResult> chat(String system, List<ChatMessage> messages, int maxTokens) {
        if (!settings.hasApiKey()) {
            return CompletableFuture.completedFuture(AiResult.fail(AiResult.Type.NO_KEY, "Chưa cấu hình API Key"));
        }
        if (!settings.isUrlSecure()) {
            return CompletableFuture.completedFuture(AiResult.fail(AiResult.Type.INSECURE_URL, "API URL không phải HTTPS"));
        }

        HttpRequest request;
        try {
            request = buildRequest(system, messages, maxTokens);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(AiResult.fail(AiResult.Type.BAD_REQUEST, "API URL không hợp lệ"));
        }

        try {
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(this::handleResponse)
                    .exceptionally(this::handleFailure);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(handleFailure(e));
        }
    }

    @Override
    public void close() {
        try {
            http.shutdownNow();
        } catch (RuntimeException ignored) {
            // Đang tắt plugin: bỏ qua mọi lỗi khi đóng.
        }
    }

    // ------------------------------------------------------------------ request

    private HttpRequest buildRequest(String system, List<ChatMessage> messages, int maxTokens) {
        boolean openai = settings.apiFormat.equals("openai");

        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model);
        body.addProperty("max_tokens", maxTokens);

        JsonArray array = new JsonArray();
        if (openai) {
            if (system != null && !system.isEmpty()) {
                array.add(message("system", system));
            }
        } else if (system != null && !system.isEmpty()) {
            body.addProperty("system", system);
        }
        for (ChatMessage m : messages) {
            array.add(message(m.role(), m.content()));
        }
        body.add("messages", array);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(settings.apiUrl))
                .timeout(Duration.ofSeconds(settings.timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");

        if (openai) {
            builder.header("Authorization", "Bearer " + settings.apiKey);
        } else {
            builder.header("x-api-key", settings.apiKey);
            builder.header("anthropic-version", settings.anthropicVersion);
        }

        return builder
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
    }

    private static JsonObject message(String role, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", role);
        obj.addProperty("content", content);
        return obj;
    }

    // ----------------------------------------------------------------- response

    private AiResult handleResponse(HttpResponse<String> response) {
        int code = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        if (code >= 200 && code < 300) {
            try {
                String text = extractText(body);
                if (text == null || text.isBlank()) {
                    return AiResult.fail(AiResult.Type.EMPTY, "Phản hồi rỗng");
                }
                return AiResult.ok(text);
            } catch (RuntimeException e) {
                return AiResult.fail(AiResult.Type.BAD_RESPONSE, "Không đọc được phản hồi JSON");
            }
        }

        String message = extractErrorMessage(body);
        String lower = message.toLowerCase(Locale.ROOT);
        boolean quotaHint = lower.contains("credit") || lower.contains("quota")
                || lower.contains("billing") || lower.contains("insufficient");
        String detail = "HTTP " + code + (message.isEmpty() ? "" : " - " + redact(message));

        AiResult.Type type;
        if (code == 401 || code == 403) {
            type = AiResult.Type.INVALID_KEY;
        } else if (code == 402) {
            type = AiResult.Type.QUOTA;
        } else if (code == 429) {
            type = quotaHint ? AiResult.Type.QUOTA : AiResult.Type.RATE_LIMIT;
        } else if (code == 400) {
            type = quotaHint ? AiResult.Type.QUOTA : AiResult.Type.BAD_REQUEST;
        } else if (code == 408) {
            type = AiResult.Type.TIMEOUT;
        } else if (code >= 500) {
            type = AiResult.Type.SERVER;
        } else {
            type = AiResult.Type.BAD_REQUEST;
        }
        return AiResult.fail(type, detail);
    }

    private AiResult handleFailure(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof HttpTimeoutException) {
            return AiResult.fail(AiResult.Type.TIMEOUT, "Hết thời gian chờ (" + settings.timeoutSeconds + "s)");
        }
        if (cause instanceof RejectedExecutionException) {
            return AiResult.fail(AiResult.Type.NETWORK, "Plugin đang tắt");
        }
        String name = cause.getClass().getSimpleName();
        String msg = cause.getMessage() == null ? "" : " - " + cause.getMessage();
        return AiResult.fail(AiResult.Type.NETWORK, redact(name + msg));
    }

    private String extractText(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        StringBuilder out = new StringBuilder();

        if (settings.apiFormat.equals("openai")) {
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JsonObject msg = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            JsonElement content = msg == null ? null : msg.get("content");
            return content == null || content.isJsonNull() ? null : content.getAsString();
        }

        JsonArray content = root.getAsJsonArray("content");
        if (content == null) {
            return null;
        }
        for (JsonElement element : content) {
            JsonObject block = element.getAsJsonObject();
            JsonElement type = block.get("type");
            JsonElement text = block.get("text");
            if (type != null && "text".equals(type.getAsString()) && text != null && !text.isJsonNull()) {
                out.append(text.getAsString());
            }
        }
        return out.toString();
    }

    private String extractErrorMessage(String body) {
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                JsonElement error = parsed.getAsJsonObject().get("error");
                if (error != null && error.isJsonObject()) {
                    JsonElement message = error.getAsJsonObject().get("message");
                    if (message != null && !message.isJsonNull()) {
                        return message.getAsString().trim();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Body không phải JSON hợp lệ.
        }
        return "";
    }

    /** Che API Key (nếu vô tình xuất hiện) và rút gọn trước khi ghi log. */
    private String redact(String text) {
        String out = text == null ? "" : text;
        if (!settings.apiKey.isEmpty()) {
            out = out.replace(settings.apiKey, "***");
        }
        return out.length() > 200 ? out.substring(0, 200) + "..." : out;
    }
}
