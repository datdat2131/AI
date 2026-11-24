package com.astrax.ai.gui;

import com.astrax.ai.AstraxAI;
import com.astrax.ai.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Menu bảng giá rank + hướng dẫn thanh toán (đọc từ shop.yml).
 * Chỉ hiển thị thông tin; việc gift rank do staff thực hiện thủ công.
 */
public final class RankMenu {

    public static final int SIZE = 36;
    public static final int[] RANK_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    public static final int SLOT_BANK = 20;
    public static final int SLOT_CARD = 22;
    public static final int SLOT_HELP = 24;
    public static final int SLOT_BACK = 30;
    public static final int SLOT_CLOSE = 32;

    /** Từ khóa (đã bỏ dấu, viết thường) cho thấy người chơi đang hỏi về rank/nạp tiền. */
    private static final Pattern SHOP_WORDS = Pattern.compile(
            "(^|[^a-z])(rank|nap|donate|bang gia|chuyen khoan|bank)([^a-z]|$)");

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public record Rank(String id, String name, String bankPrice, String cardPrice,
                       Material material, List<String> perks) {
    }

    private final AstraxAI plugin;

    private volatile boolean enabled = true;
    private volatile boolean intercept = true;
    private volatile String qrUrl = "";
    private volatile String discordUrl = "";
    private volatile String bankName = "";
    private volatile String account = "";
    private volatile String holderName = "";
    private volatile String note = "";
    private volatile List<Rank> ranks = List.of();

    public RankMenu(AstraxAI plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled && !ranks.isEmpty();
    }

    /** Nạp shop.yml. Trả về false nếu file lỗi. */
    public boolean load() {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Không đọc được shop.yml: " + e.getMessage());
            return false;
        }

        this.enabled = yaml.getBoolean("enabled", true);
        this.intercept = yaml.getBoolean("intercept-chat", true);
        this.qrUrl = httpsOrEmpty(yaml.getString("qr-url", ""));
        this.discordUrl = httpsOrEmpty(yaml.getString("discord-url", ""));
        this.bankName = str(yaml.getString("bank.name", ""));
        this.account = str(yaml.getString("bank.account", ""));
        this.holderName = str(yaml.getString("bank.holder", ""));
        this.note = str(yaml.getString("transfer-note", ""));

        List<Rank> list = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("ranks");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection r = section.getConfigurationSection(id);
                if (r == null) {
                    continue;
                }
                if (list.size() >= RANK_SLOTS.length) {
                    plugin.getLogger().warning("shop.yml có hơn " + RANK_SLOTS.length
                            + " rank, các rank thừa sẽ bị bỏ qua.");
                    break;
                }
                Material material = Material.matchMaterial(r.getString("material", "GOLD_INGOT"));
                if (material == null || !material.isItem()) {
                    material = Material.GOLD_INGOT;
                }
                list.add(new Rank(id, str(r.getString("name", id)),
                        str(r.getString("bank-price", "?")), str(r.getString("card-price", "?")),
                        material, r.getStringList("perks")));
            }
        }
        this.ranks = List.copyOf(list);
        return true;
    }

    /** Câu hỏi này có phải hỏi về rank/nạp tiền không (để mở menu thay vì gọi AI). */
    public boolean shouldIntercept(String message) {
        if (!isEnabled() || !intercept || message == null) {
            return false;
        }
        String s = Normalizer.normalize(message.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
        return SHOP_WORDS.matcher(s).find();
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, Text.color("&8Bảng giá Rank &7• &6Nạp rank"));
        holder.inventory = inv;

        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, "&r ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        List<Rank> current = ranks;
        for (int i = 0; i < current.size() && i < RANK_SLOTS.length; i++) {
            Rank rank = current.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("&7Chuyển khoản: &a" + rank.bankPrice());
            lore.add("&7Thẻ cào: &e" + rank.cardPrice());
            lore.add("");
            if (rank.perks().isEmpty()) {
                lore.add("&7Quyền lợi: hãy hỏi staff");
            } else {
                for (String perk : rank.perks()) {
                    lore.add("&7• &f" + perk);
                }
            }
            lore.add("");
            lore.add("&eNhấn để xem cách thanh toán");
            inv.setItem(RANK_SLOTS[i], icon(rank.material(), "&6&l" + rank.name(), lore.toArray(new String[0])));
        }

        inv.setItem(SLOT_BANK, icon(Material.PAPER, "&a&lChuyển khoản ngân hàng",
                "&7Xem số tài khoản và mã QR.", "&7Chuyển ĐÚNG số tiền của rank.", "", "&eNhấn để xem"));
        inv.setItem(SLOT_CARD, icon(Material.GOLD_NUGGET, "&e&lNạp thẻ cào",
                "&7Tính năng đang được cập nhật.", "", "&eNhấn để xem hướng dẫn"));
        inv.setItem(SLOT_HELP, icon(Material.BOOK, "&c&lĐã chuyển khoản chưa nhận rank?",
                "&7Liên hệ staff để được kiểm tra.", "", "&eNhấn để xem"));
        inv.setItem(SLOT_BACK, icon(Material.ARROW, "&7Quay lại menu chính"));
        inv.setItem(SLOT_CLOSE, icon(Material.BARRIER, "&cĐóng"));

        player.openInventory(inv);
    }

    /** Xử lý click trong menu rank (gọi trên main thread). */
    public void handleClick(Player player, int slot) {
        List<Rank> current = ranks;
        for (int i = 0; i < current.size() && i < RANK_SLOTS.length; i++) {
            if (slot == RANK_SLOTS[i]) {
                player.closeInventory();
                sendPayment(player, current.get(i));
                return;
            }
        }
        if (slot == SLOT_BANK) {
            player.closeInventory();
            sendPayment(player, null);
        } else if (slot == SLOT_CARD) {
            player.closeInventory();
            Text.send(player, "&eNạp bằng thẻ cào: tính năng đang được cập nhật. Hãy ib admin để được hỗ trợ.");
            sendContact(player);
        } else if (slot == SLOT_HELP) {
            player.closeInventory();
            Text.send(player, "&eĐã chuyển khoản mà chưa nhận được rank? Hãy ib staff hoặc owner để được kiểm tra.");
            sendContact(player);
        } else if (slot == SLOT_BACK) {
            if (player.hasPermission("astraxai.gui")) {
                plugin.menu().open(player);
            } else {
                player.closeInventory();
            }
        } else if (slot == SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    private void sendPayment(Player player, Rank rank) {
        Text.sendRaw(player, rank == null
                ? "&b===== Thanh toán bằng chuyển khoản ====="
                : "&b===== Thanh toán rank " + rank.name() + " =====");
        if (rank != null) {
            player.sendMessage(line("Số tiền chuyển khoản: ", rank.bankPrice(), NamedTextColor.GREEN));
        }
        player.sendMessage(line("Ngân hàng: ", bankName, NamedTextColor.WHITE));
        if (!account.isEmpty()) {
            player.sendMessage(Component.text("Số tài khoản: ", NamedTextColor.GRAY)
                    .append(Component.text(account, NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.copyToClipboard(account))
                            .hoverEvent(HoverEvent.showText(Component.text("Bấm để sao chép", NamedTextColor.GRAY))))
                    .append(Component.text("  [bấm để sao chép]", NamedTextColor.DARK_GRAY)));
        }
        player.sendMessage(line("Chủ tài khoản: ", holderName, NamedTextColor.WHITE));
        if (!note.isEmpty()) {
            player.sendMessage(Component.text(note, NamedTextColor.YELLOW));
        }
        if (!qrUrl.isEmpty()) {
            player.sendMessage(Component.text("Mã QR: ", NamedTextColor.GRAY).append(Text.link("[Bấm để mở mã QR]", qrUrl)));
        }
        Text.sendRaw(player, "&aChuyển khoản xong, staff sẽ gift rank cho bạn.");
    }

    private void sendContact(Player player) {
        if (!discordUrl.isEmpty()) {
            player.sendMessage(Component.text("Liên hệ staff: ", NamedTextColor.GRAY)
                    .append(Text.link("[Bấm để mở Discord]", discordUrl)));
        }
    }

    private static Component line(String label, String value, NamedTextColor valueColor) {
        return Component.text(label, NamedTextColor.GRAY).append(Component.text(value, valueColor));
    }

    private static ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) {
                lines.add(Text.item(l));
            }
            meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String str(String s) {
        return s == null ? "" : s.trim();
    }

    private static String httpsOrEmpty(String url) {
        String s = str(url);
        return s.toLowerCase(Locale.ROOT).startsWith("https://") ? s : "";
    }
}
