package com.astrax.ai;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Menu GUI chính của AstraxAI, dùng Inventory của Bukkit/Paper. */
public final class MainMenu {

    public static final int SIZE = 27;
    public static final int SLOT_CHAT = 10;
    public static final int SLOT_GUIDE = 11;
    public static final int SLOT_BOSS = 12;
    public static final int SLOT_RELIC = 13;
    public static final int SLOT_RANK = 14;
    public static final int SLOT_ADMIN = 16;
    public static final int SLOT_CLOSE = 22;

    /** Holder riêng để nhận biết chính xác inventory của plugin này. */
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final AstraxAI plugin;

    public MainMenu(AstraxAI plugin) {
        this.plugin = plugin;
    }

    /** Người chơi có được thấy nút Trợ lý Admin hay không. */
    public boolean canSeeAdmin(Player player) {
        return plugin.settings().adminEnabled && player.hasPermission("astraxai.admin");
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Text.color("&8AstraxAI &7• &bTrợ lý AI"));
        holder.inventory = inventory;

        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, "&r ");
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(SLOT_CHAT, icon(Material.WRITABLE_BOOK, "&a&lChat với AI",
                "&7Đặt câu hỏi bất kỳ cho trợ lý AI.", "&7Sau khi bấm, hãy gõ câu hỏi vào khung chat.", "", "&eNhấn để bắt đầu"));
        inventory.setItem(SLOT_GUIDE, icon(Material.BOOK, "&e&lHướng dẫn server",
                "&7AI hướng dẫn cách chơi trên server.", "", "&eNhấn để hỏi"));
        inventory.setItem(SLOT_BOSS, icon(Material.WITHER_SKELETON_SKULL, "&c&lHỏi về Boss",
                "&7Tìm hiểu các boss và cách đánh.", "", "&eNhấn để hỏi"));
        inventory.setItem(SLOT_RELIC, icon(Material.NETHER_STAR, "&d&lHỏi về Relic",
                "&7Tìm hiểu cách lấy và tác dụng của Relic.", "", "&eNhấn để hỏi"));
        inventory.setItem(SLOT_RANK, icon(Material.GOLD_INGOT, "&6&lHỏi về Rank",
                "&7Tìm hiểu các rank và quyền lợi.", "", "&eNhấn để hỏi"));
        if (canSeeAdmin(player)) {
            inventory.setItem(SLOT_ADMIN, icon(Material.COMMAND_BLOCK, "&4&lTrợ lý Admin",
                    "&7AI chẩn đoán tình trạng server.", "&cChỉ dành cho admin.", "", "&eNhấn để chẩn đoán"));
        }
        inventory.setItem(SLOT_CLOSE, icon(Material.BARRIER, "&cĐóng"));

        player.openInventory(inventory);
    }

    private static ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(name));
        if (lore.length > 0) {
            List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Text.item(line));
            }
            meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }
}
