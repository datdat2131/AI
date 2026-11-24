package com.astrax.ai;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Danh sách cảnh báo gần đây mà chính plugin ghi nhận (ví dụ lỗi API AI).
 * Đây là nguồn "cảnh báo được phép truy cập" của Admin AI: AI không đọc log server.
 */
public final class WarningTracker {

    private static final int MAX_ITEMS = 20;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Deque<String> items = new ArrayDeque<>();

    public synchronized void add(String text) {
        if (items.size() >= MAX_ITEMS) {
            items.removeFirst();
        }
        items.addLast(LocalTime.now().format(TIME) + " " + text);
    }

    public synchronized List<String> recent() {
        return new ArrayList<>(items);
    }

    public synchronized void clear() {
        items.clear();
    }
}
