package com.astrax.ai.chat;

import com.astrax.ai.AstraxAI;
import com.astrax.ai.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Bắt tin nhắn chat sau khi bấm nút "Chat với AI", và dọn dữ liệu khi người chơi thoát. */
public final class ChatListener implements Listener {

    private final AstraxAI plugin;

    public ChatListener(AstraxAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.chatInput().consume(player.getUniqueId())) {
            return;
        }
        // Tin nhắn này dành cho AI, không phát lên chat chung.
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("huy") || text.equalsIgnoreCase("hủy") || text.equalsIgnoreCase("cancel")) {
                Text.send(player, "&7Đã hủy câu hỏi.");
                return;
            }
            plugin.chatService().ask(player, text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        plugin.chatInput().cancel(id);
        plugin.chatService().onQuit(id);
    }
}
