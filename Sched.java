package com.astrax.ai;

import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

/** Chuyển công việc về main thread một cách an toàn (bỏ qua nếu plugin đã tắt). */
public final class Sched {

    private Sched() {
    }

    public static void sync(Plugin plugin, Runnable task) {
        if (!plugin.isEnabled()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
        } catch (IllegalPluginAccessException ignored) {
            // Plugin đang tắt: không còn gì để gửi.
        }
    }
}
