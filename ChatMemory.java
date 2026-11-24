package com.astrax.ai;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lịch sử hội thoại riêng cho từng người chơi (khóa theo UUID), chỉ lưu trong RAM,
 * không bao giờ ghi ra đĩa. Có giới hạn số tin nhắn và có thể bật/tắt.
 */
public final class ChatMemory {

    /** Ảnh chụp lịch sử tại thời điểm bắt đầu một yêu cầu. */
    public record Snapshot(List<ChatMessage> messages, int epoch) {
    }

    private static final class Session {
        final List<ChatMessage> messages = new ArrayList<>();
        int epoch;

        Session(int epoch) {
            this.epoch = epoch;
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger epochCounter = new AtomicInteger();
    private volatile boolean enabled;
    private volatile int maxMessages;

    public ChatMemory(boolean enabled, int maxMessages) {
        configure(enabled, maxMessages);
    }

    public void configure(boolean enabled, int maxMessages) {
        this.enabled = enabled;
        this.maxMessages = Math.max(2, maxMessages);
        if (!enabled) {
            sessions.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Lấy lịch sử hiện tại của đúng người chơi này. */
    public Snapshot snapshot(UUID id) {
        if (!enabled) {
            return new Snapshot(List.of(), 0);
        }
        Session session = sessions.computeIfAbsent(id, k -> new Session(epochCounter.incrementAndGet()));
        synchronized (session) {
            return new Snapshot(new ArrayList<>(session.messages), session.epoch);
        }
    }

    /**
     * Thêm một cặp hỏi-đáp. Bỏ qua nếu bộ nhớ tắt, phiên đã bị xóa/thoát,
     * hoặc lịch sử đã bị xóa (epoch thay đổi) trong lúc chờ AI trả lời.
     */
    public void append(UUID id, int epoch, String userText, String assistantText) {
        if (!enabled) {
            return;
        }
        Session session = sessions.get(id);
        if (session == null) {
            return;
        }
        synchronized (session) {
            if (session.epoch != epoch) {
                return;
            }
            session.messages.add(new ChatMessage("user", userText));
            session.messages.add(new ChatMessage("assistant", assistantText));
            // Cắt theo cặp để lịch sử luôn bắt đầu bằng tin nhắn của người chơi.
            while (session.messages.size() > maxMessages && session.messages.size() >= 2) {
                session.messages.remove(0);
                session.messages.remove(0);
            }
        }
    }

    /** /ai clear: xóa lịch sử và vô hiệu hóa các câu trả lời đang chờ. */
    public void clear(UUID id) {
        Session session = sessions.get(id);
        if (session == null) {
            return;
        }
        synchronized (session) {
            session.messages.clear();
            session.epoch = epochCounter.incrementAndGet();
        }
    }

    /** Gỡ hoàn toàn phiên của người chơi (khi thoát server). */
    public void remove(UUID id) {
        sessions.remove(id);
    }

    public void clearAll() {
        sessions.clear();
    }

    public int size(UUID id) {
        Session session = sessions.get(id);
        if (session == null) {
            return 0;
        }
        synchronized (session) {
            return session.messages.size();
        }
    }
}
