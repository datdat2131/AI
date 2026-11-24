package com.astrax.ai.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/** Tiện ích hiển thị văn bản. Nội dung do AI sinh ra luôn là văn bản thuần (không mã màu, không click). */
public final class Text {

    public static final String PREFIX = "&b[AstraxAI] &f";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /** Chuyển chuỗi có mã màu '&' thành Component (chỉ dùng cho chuỗi do plugin tự viết). */
    public static Component color(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    /** Component văn bản thuần, không diễn giải mã màu. */
    public static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    public static void send(CommandSender to, String legacy) {
        to.sendMessage(color(PREFIX + legacy));
    }

    public static void sendRaw(CommandSender to, String legacy) {
        to.sendMessage(color(legacy));
    }

    /** Tên/lore của vật phẩm GUI: tắt chữ nghiêng mặc định. */
    public static Component item(String legacy) {
        return LEGACY.deserialize(legacy).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Gửi câu trả lời của AI từng dòng, dưới dạng văn bản thuần. */
    public static void sendAi(CommandSender to, String aiText) {
        for (String line : cleanAiText(aiText).split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            to.sendMessage(Component.text("[AI] ", NamedTextColor.AQUA)
                    .append(Component.text(line, NamedTextColor.WHITE)));
        }
    }

    /** Loại bỏ markdown và ký tự điều khiển vì khung chat Minecraft không hiển thị markdown. */
    public static String cleanAiText(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r", "");
        s = s.replaceAll("[\\p{Cntrl}&&[^\n]]", "");
        s = s.replace("**", "").replace("__", "").replace("`", "");
        s = s.replaceAll("(?m)^[ \t]{0,3}#{1,6}[ \t]*", "");
        s = s.replaceAll("(?m)^[ \t]*[-*][ \t]+", "• ");
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.trim();
    }
}
