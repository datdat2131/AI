package com.astrax.ai.chat;

import com.astrax.ai.AstraxAI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Đọc plugins/AstraxAI/knowledge.yml và chuyển thành văn bản để đưa vào system prompt.
 * Chỉ đọc đúng file này, không bao giờ đọc file nào khác theo yêu cầu của AI hay người chơi.
 */
public final class KnowledgeBase {

    private static final String[][] SECTIONS = {
            {"server", "THÔNG TIN SERVER"},
            {"features", "TÍNH NĂNG"},
            {"ranks", "RANK"},
            {"bosses", "BOSS"},
            {"relics", "RELIC"},
            {"quests", "QUEST"},
            {"rules", "LUẬT SERVER"},
            {"guide", "HƯỚNG DẪN NGƯỜI CHƠI"},
            {"extra", "GHI CHÚ KHÁC"}
    };

    private final AstraxAI plugin;
    private volatile String rendered = "";
    private volatile String serverName = "Server Minecraft";

    public KnowledgeBase(AstraxAI plugin) {
        this.plugin = plugin;
    }

    public String render() {
        return rendered;
    }

    public String serverName() {
        return serverName;
    }

    /** Nạp (hoặc nạp lại) knowledge.yml. Trả về false nếu file lỗi. */
    public boolean load(int maxChars) {
        File file = new File(plugin.getDataFolder(), "knowledge.yml");
        if (!file.exists()) {
            plugin.saveResource("knowledge.yml", false);
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Không đọc được knowledge.yml: " + e.getMessage());
            rendered = "(Không đọc được dữ liệu server.)";
            return false;
        }

        String name = yaml.getString("server.name", "Server Minecraft");
        serverName = name == null || name.isBlank() ? "Server Minecraft" : name.trim();

        StringBuilder sb = new StringBuilder();
        for (String[] section : SECTIONS) {
            Object value = yaml.get(section[0]);
            if (value == null) {
                continue;
            }
            sb.append("## ").append(section[1]).append('\n');
            renderObject(sb, value, 0);
            sb.append('\n');
        }

        String text = sb.toString().trim();
        if (text.isEmpty()) {
            text = "(Admin chưa cấu hình dữ liệu server.)";
        }
        if (text.length() > maxChars) {
            plugin.getLogger().warning("knowledge.yml quá dài (" + text.length()
                    + " ký tự), chỉ gửi " + maxChars + " ký tự đầu cho AI.");
            text = text.substring(0, maxChars) + "\n(... dữ liệu bị cắt bớt vì quá dài)";
        }
        rendered = text;
        return true;
    }

    private void renderObject(StringBuilder sb, Object value, int indent) {
        String pad = "  ".repeat(indent);
        if (value instanceof ConfigurationSection section) {
            for (String key : section.getKeys(false)) {
                renderEntry(sb, pad, key, section.get(key), indent);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                renderEntry(sb, pad, String.valueOf(entry.getKey()), entry.getValue(), indent);
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                if (isContainer(item)) {
                    sb.append(pad).append("-\n");
                    renderObject(sb, item, indent + 1);
                } else {
                    sb.append(pad).append("- ").append(item).append('\n');
                }
            }
        } else {
            sb.append(pad).append(value).append('\n');
        }
    }

    private void renderEntry(StringBuilder sb, String pad, String key, Object value, int indent) {
        if (isContainer(value)) {
            sb.append(pad).append(key).append(":\n");
            renderObject(sb, value, indent + 1);
        } else {
            sb.append(pad).append(key).append(": ").append(value).append('\n');
        }
    }

    private static boolean isContainer(Object value) {
        return value instanceof ConfigurationSection || value instanceof Map<?, ?> || value instanceof List<?>;
    }
}
