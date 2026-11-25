package com.astrax.ai.chat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Biến danh sách plugin và lệnh thành đoạn văn bản gọn để đưa vào prompt của AI.
 * Lớp này không phụ thuộc Bukkit nên kiểm tra riêng được. Mọi chuỗi đều được làm sạch
 * (một dòng, bỏ ký tự điều khiển và mã màu) vì mô tả plugin là dữ liệu của bên thứ ba.
 */
public final class PluginDigest {

    public record Cmd(String name, String usage, String description, List<String> aliases, boolean publicCmd) {
    }

    public record Entry(String name, String version, String description, List<Cmd> commands) {
    }

    public record Result(String text, int plugins, int commands, boolean truncated, List<String> summary) {
        public static Result empty() {
            return new Result("", 0, 0, false, List.of());
        }
    }

    private PluginDigest() {
    }

    /**
     * @param publicOnly chỉ liệt kê lệnh người chơi thường dùng được; plugin không có lệnh công khai nào bị bỏ qua
     * @param maxChars   giới hạn tổng số ký tự của văn bản
     * @param maxCommands số lệnh tối đa hiển thị cho mỗi plugin
     */
    public static Result build(List<Entry> entries, boolean publicOnly, int maxChars, int maxCommands) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));

        StringBuilder out = new StringBuilder();
        List<String> summary = new ArrayList<>();
        int plugins = 0;
        int commands = 0;
        boolean truncated = false;

        for (Entry entry : sorted) {
            List<Cmd> visible = new ArrayList<>();
            for (Cmd c : entry.commands()) {
                if (!publicOnly || c.publicCmd()) {
                    visible.add(c);
                }
            }
            if (publicOnly && visible.isEmpty()) {
                continue;
            }
            visible.sort(Comparator.comparing(c -> c.name().toLowerCase(Locale.ROOT)));

            StringBuilder block = new StringBuilder();
            block.append("- ").append(clean(entry.name(), 40));
            String version = clean(entry.version(), 20);
            if (!version.isEmpty()) {
                block.append(' ').append(version);
            }
            String description = clean(entry.description(), 160);
            if (!description.isEmpty()) {
                block.append(": ").append(description);
            }
            block.append('\n');

            int shown = Math.min(visible.size(), Math.max(0, maxCommands));
            for (int i = 0; i < shown; i++) {
                block.append("  ").append(describe(visible.get(i))).append('\n');
            }
            if (visible.size() > shown) {
                block.append("  (... và ").append(visible.size() - shown).append(" lệnh khác)\n");
            }

            if (out.length() + block.length() > maxChars) {
                truncated = true;
                break;
            }
            out.append(block);
            plugins++;
            commands += shown;
            summary.add(clean(entry.name(), 40) + " (" + visible.size() + " lệnh)");
        }

        if (truncated) {
            out.append("(... còn plugin khác bị cắt bớt do giới hạn độ dài)\n");
        }
        return new Result(out.toString().trim(), plugins, commands, truncated, summary);
    }

    private static String describe(Cmd c) {
        String name = clean(c.name(), 40);
        String usage = clean(c.usage(), 100).replace("<command>", name);
        StringBuilder sb = new StringBuilder(usage.isEmpty() ? "/" + name : usage);

        String description = clean(c.description(), 120);
        if (!description.isEmpty()) {
            sb.append(" - ").append(description);
        }
        List<String> aliases = new ArrayList<>();
        for (String alias : c.aliases()) {
            String a = clean(alias, 30);
            if (!a.isEmpty() && aliases.size() < 5) {
                aliases.add("/" + a);
            }
        }
        if (!aliases.isEmpty()) {
            sb.append(" (bí danh: ").append(String.join(", ", aliases)).append(')');
        }
        return sb.toString();
    }

    /** Một dòng, không ký tự điều khiển, không mã màu, giới hạn độ dài. */
    public static String clean(String text, int max) {
        if (text == null) {
            return "";
        }
        String s = text.replaceAll("§.?", " ").replaceAll("[\\p{Cntrl}\\s]+", " ").trim();
        if (s.length() > max) {
            s = s.substring(0, Math.max(0, max - 1)).trim() + "…";
        }
        return s;
    }
}
