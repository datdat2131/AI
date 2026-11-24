package com.astrax.ai.chat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Theo dõi những người chơi vừa bấm nút "Chat với AI" trong menu và đang được
 * mời gõ câu hỏi tiếp theo vào khung chat.
 */
public final class ChatInputManager {

    private final Map<UUID, Long> waiting = new ConcurrentHashMap<>();

    public void begin(UUID id, int seconds) {
        waiting.put(id, System.currentTimeMillis() + seconds * 1000L);
    }

    /** Trả về true (và gỡ trạng thái chờ) nếu người chơi đang chờ nhập và chưa hết hạn. */
    public boolean consume(UUID id) {
        Long until = waiting.remove(id);
        return until != null && until >= System.currentTimeMillis();
    }

    public void cancel(UUID id) {
        waiting.remove(id);
    }

    public void clear() {
        waiting.clear();
    }
}
