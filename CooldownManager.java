package com.astrax.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Quản lý thời gian chờ (cooldown) theo UUID, an toàn khi dùng từ nhiều luồng. */
public final class CooldownManager {

    private final Map<UUID, Long> expiry = new ConcurrentHashMap<>();

    /** Số mili giây còn phải chờ; 0 nếu đã hết cooldown. */
    public long remainingMillis(UUID id) {
        Long until = expiry.get(id);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            expiry.remove(id, until);
            return 0L;
        }
        return remaining;
    }

    public void mark(UUID id, long durationMillis) {
        if (durationMillis <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (expiry.size() > 500) {
            expiry.values().removeIf(until -> until <= now);
        }
        expiry.put(id, now + durationMillis);
    }

    public void clear() {
        expiry.clear();
    }

    public static long toSeconds(long millis) {
        return Math.max(1L, (millis + 999L) / 1000L);
    }
}
