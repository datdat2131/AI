package com.astrax.ai.admin;

import com.astrax.ai.AstraxAI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Thu thập số liệu server mà plugin được phép đọc: TPS, MSPT, người chơi, RAM, plugin, thế giới.
 * Chỉ đọc, không thay đổi bất cứ thứ gì. {@link #collect()} PHẢI được gọi từ main thread.
 */
public final class ServerMonitor {

    public record WorldInfo(String name, int loadedChunks, int entities, int players) {
    }

    public record Snapshot(String serverVersion, long uptimeMinutes,
                           double tps1, double tps5, double tps15, double mspt,
                           int online, int maxPlayers,
                           long usedMb, long allocatedMb, long maxMb,
                           List<String> enabledPlugins, List<String> disabledPlugins,
                           List<WorldInfo> worlds, List<String> warnings) {
        public double ramPercent() {
            return maxMb <= 0 ? 0.0 : usedMb * 100.0 / maxMb;
        }
    }

    private final AstraxAI plugin;

    public ServerMonitor(AstraxAI plugin) {
        this.plugin = plugin;
    }

    public Snapshot collect() {
        double[] tps = Bukkit.getTPS();
        double t1 = tps.length > 0 ? Math.min(20.0, tps[0]) : 20.0;
        double t5 = tps.length > 1 ? Math.min(20.0, tps[1]) : t1;
        double t15 = tps.length > 2 ? Math.min(20.0, tps[2]) : t5;
        double mspt = Bukkit.getAverageTickTime();

        Runtime rt = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        long max = rt.maxMemory() / mb;
        long allocated = rt.totalMemory() / mb;
        long used = (rt.totalMemory() - rt.freeMemory()) / mb;

        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String entry = p.getPluginMeta().getName() + " " + p.getPluginMeta().getVersion();
            if (p.isEnabled()) {
                enabled.add(entry);
            } else {
                disabled.add(entry);
            }
        }

        List<WorldInfo> worlds = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            worlds.add(new WorldInfo(w.getName(), w.getLoadedChunks().length,
                    w.getEntities().size(), w.getPlayers().size()));
        }

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 60000L;

        List<String> warnings = new ArrayList<>();
        if (t1 < 15.0) {
            warnings.add("TPS rất thấp (" + fmt(t1) + "), server đang lag nặng.");
        } else if (t1 < 18.0) {
            warnings.add("TPS thấp (" + fmt(t1) + "), server có dấu hiệu lag.");
        }
        if (mspt > 50.0) {
            warnings.add("MSPT vượt 50ms (" + fmt(mspt) + "ms), tick không kịp trong 1/20 giây.");
        } else if (mspt > 40.0) {
            warnings.add("MSPT cao (" + fmt(mspt) + "ms), gần ngưỡng 50ms.");
        }
        double ramPct = max <= 0 ? 0.0 : used * 100.0 / max;
        if (ramPct > 90.0) {
            warnings.add("RAM sử dụng rất cao (" + fmt(ramPct) + "%).");
        } else if (ramPct > 80.0) {
            warnings.add("RAM sử dụng cao (" + fmt(ramPct) + "%).");
        }
        if (!disabled.isEmpty()) {
            warnings.add("Có " + disabled.size() + " plugin đang bị tắt hoặc lỗi khi tải: "
                    + String.join(", ", disabled));
        }
        for (WorldInfo w : worlds) {
            if (w.entities() > 5000) {
                warnings.add("Thế giới " + w.name() + " có rất nhiều thực thể (" + w.entities() + ").");
            }
        }
        for (String w : plugin.warnings().recent()) {
            warnings.add("AstraxAI ghi nhận: " + w);
        }

        return new Snapshot(Bukkit.getVersion(), uptime, t1, t5, t15, mspt,
                Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers(),
                used, allocated, max, enabled, disabled, worlds, warnings);
    }

    /** Dòng hiển thị (có mã màu '&') cho lệnh /astraai status. Không chứa tên plugin. */
    public List<String> toLegacyLines(Snapshot s) {
        List<String> lines = new ArrayList<>();
        lines.add("&b===== Trạng thái server =====");
        lines.add("&7TPS (1m/5m/15m): " + tpsColor(s.tps1()) + fmt(s.tps1()) + "&7 / " + fmt(s.tps5()) + " / " + fmt(s.tps15()));
        lines.add("&7MSPT trung bình: " + msptColor(s.mspt()) + fmt(s.mspt()) + " ms");
        lines.add("&7Người chơi: &f" + s.online() + "/" + s.maxPlayers());
        lines.add("&7RAM: " + ramColor(s.ramPercent()) + s.usedMb() + " MB &7/ &f" + s.maxMb() + " MB &7("
                + fmt(s.ramPercent()) + "%)");
        lines.add("&7Thời gian chạy: &f" + (s.uptimeMinutes() / 60) + " giờ " + (s.uptimeMinutes() % 60) + " phút");
        lines.add("&7Plugin hoạt động: &f" + s.enabledPlugins().size()
                + (s.disabledPlugins().isEmpty() ? "" : " &7| Bị tắt: &c" + s.disabledPlugins().size()));
        return lines;
    }

    /** Văn bản thuần dùng làm dữ liệu cho AI và cho file báo cáo. */
    public String toPlainText(Snapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Phiên bản server: ").append(s.serverVersion()).append('\n');
        sb.append("Thời gian chạy: ").append(s.uptimeMinutes() / 60).append(" giờ ")
                .append(s.uptimeMinutes() % 60).append(" phút\n");
        sb.append("TPS (1m/5m/15m): ").append(fmt(s.tps1())).append(" / ").append(fmt(s.tps5()))
                .append(" / ").append(fmt(s.tps15())).append('\n');
        sb.append("MSPT trung bình: ").append(fmt(s.mspt())).append(" ms\n");
        sb.append("Người chơi: ").append(s.online()).append('/').append(s.maxPlayers()).append('\n');
        sb.append("RAM: đang dùng ").append(s.usedMb()).append(" MB / tối đa ").append(s.maxMb())
                .append(" MB (").append(fmt(s.ramPercent())).append("%), đã cấp phát ")
                .append(s.allocatedMb()).append(" MB\n");

        sb.append("Thế giới:\n");
        for (WorldInfo w : s.worlds()) {
            sb.append("- ").append(w.name()).append(": chunk đã tải ").append(w.loadedChunks())
                    .append(", thực thể ").append(w.entities())
                    .append(", người chơi ").append(w.players()).append('\n');
        }

        sb.append("Plugin đang hoạt động (").append(s.enabledPlugins().size()).append("): ")
                .append(String.join(", ", s.enabledPlugins())).append('\n');
        sb.append("Plugin bị tắt (").append(s.disabledPlugins().size()).append("): ")
                .append(s.disabledPlugins().isEmpty() ? "không có" : String.join(", ", s.disabledPlugins()))
                .append('\n');

        sb.append("Cảnh báo:\n");
        if (s.warnings().isEmpty()) {
            sb.append("- Không có cảnh báo nào.\n");
        } else {
            for (String w : s.warnings()) {
                sb.append("- ").append(w).append('\n');
            }
        }
        return sb.toString();
    }

    private static String tpsColor(double tps) {
        return tps >= 18.0 ? "&a" : tps >= 15.0 ? "&e" : "&c";
    }

    private static String msptColor(double mspt) {
        return mspt <= 40.0 ? "&a" : mspt <= 50.0 ? "&e" : "&c";
    }

    private static String ramColor(double pct) {
        return pct <= 80.0 ? "&a" : pct <= 90.0 ? "&e" : "&c";
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
