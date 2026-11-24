package com.astrax.ai.gui;

import com.astrax.ai.AstraxAI;
import com.astrax.ai.chat.ChatService;
import com.astrax.ai.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Xử lý thao tác trong menu AstraxAI. Mọi thao tác nhặt/di chuyển vật phẩm đều bị chặn. */
public final class GuiListener implements Listener {

    private final AstraxAI plugin;

    public GuiListener(AstraxAI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        if (!isOurs(top)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !isOurs(clicked.getHolder())) {
            return;
        }
        final int slot = event.getSlot();
        final boolean rankMenu = top instanceof RankMenu.Holder;
        // Thực hiện ở tick sau để tránh đóng inventory ngay trong lúc xử lý sự kiện click.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (rankMenu) {
                plugin.rankMenu().handleClick(player, slot);
            } else {
                handle(player, slot);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (isOurs(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    private static boolean isOurs(InventoryHolder holder) {
        return holder instanceof MainMenu.Holder || holder instanceof RankMenu.Holder;
    }

    private void handle(Player player, int slot) {
        switch (slot) {
            case MainMenu.SLOT_CHAT -> {
                player.closeInventory();
                if (!player.hasPermission("astraxai.chat")) {
                    Text.send(player, "&cBạn không có quyền sử dụng AI Chat.");
                    return;
                }
                if (!plugin.settings().chatEnabled) {
                    Text.send(player, "&cTính năng AI Chat đang bị tắt.");
                    return;
                }
                int seconds = plugin.settings().inputTimeoutSeconds;
                plugin.chatInput().begin(player.getUniqueId(), seconds);
                Text.send(player, "&aHãy gõ câu hỏi của bạn vào khung chat (trong " + seconds
                        + " giây). Gõ &fhuy &ađể hủy.");
            }
            case MainMenu.SLOT_GUIDE -> ask(player, ChatService.QUESTION_GUIDE);
            case MainMenu.SLOT_BOSS -> ask(player, ChatService.QUESTION_BOSS);
            case MainMenu.SLOT_RELIC -> ask(player, ChatService.QUESTION_RELIC);
            case MainMenu.SLOT_RANK -> {
                if (plugin.rankMenu().isEnabled()) {
                    plugin.rankMenu().open(player);
                } else {
                    ask(player, ChatService.QUESTION_RANK);
                }
            }
            case MainMenu.SLOT_ADMIN -> {
                player.closeInventory();
                // Kiểm tra lại quyền ở thời điểm bấm, không tin vào việc nút đã hiển thị.
                if (!plugin.menu().canSeeAdmin(player)) {
                    Text.send(player, "&cBạn không có quyền sử dụng Trợ lý Admin.");
                    return;
                }
                plugin.adminService().diagnose(player);
            }
            case MainMenu.SLOT_CLOSE -> player.closeInventory();
            default -> {
                // Ô trang trí: không làm gì.
            }
        }
    }

    private void ask(Player player, String question) {
        player.closeInventory();
        plugin.chatService().ask(player, question);
    }
}
