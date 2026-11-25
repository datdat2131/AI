package com.astrax.ai.chat;

import com.astrax.ai.AstraxAI;
import com.astrax.ai.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cho AI "đọc" các plugin đang chạy trên server: tên, phiên bản, mô tả và các lệnh đã đăng ký.
 * Chỉ dùng API Bukkit ở chế độ đọc: không đọc file jar hay file cấu hình của plugin nào,
 * không đọc log, không chạy lệnh. Phải gọi {@link #refresh()} từ main thread.
 */
public final class PluginKnowledge implements Listener {

    private final AstraxAI plugin;
    private volatile PluginDigest.Result chatView = PluginDigest.Result.empty();
    private volatile PluginDigest.Result adminView = PluginDigest.Result.empty();

    public PluginKnowledge(AstraxAI plugin) {
        this.plugin = plugin;
    }

    /** Thu thập lại danh sách plugin và lệnh (main thread). */
    public void refresh() {
        Settings s = plugin.settings();
        if (!s.pluginsEnabled) {
            chatView = PluginDigest.Result.empty();
            adminView = PluginDigest.Result.empty();
            return;
        }
        try {
            List<PluginDigest.Entry> entries = collect(s);
            PluginDigest.Result publicOnly = PluginDigest.build(entries, true, s.pluginsMaxChars, s.pluginsMaxCommands);
            PluginDigest.Result everything = PluginDigest.build(entries, false, s.pluginsMaxChars, s.pluginsMaxCommands);
            chatView = s.pluginsPublicOnly ? publicOnly : everything;
            adminView = everything;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Không đọc được danh sách plugin: " + e.getClass().getSimpleName());
            chatView = PluginDigest.Result.empty();
            adminView = PluginDigest.Result.empty();
        }
    }

    /** Sau khi server khởi động xong, mọi plugin đã nạp lệnh: đọc lại để không sót plugin nào. */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        refresh();
    }

    public String forChat() {
        return chatView.text();
    }

    public String forAdmin() {
        return adminView.text();
    }

    public PluginDigest.Result chatView() {
        return chatView;
    }

    public PluginDigest.Result adminView() {
        return adminView;
    }

    private List<PluginDigest.Entry> collect(Settings s) {
        Map<Plugin, List<Command>> byPlugin = new HashMap<>();
        Set<Command> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Command command : Bukkit.getCommandMap().getKnownCommands().values()) {
            if (command instanceof PluginCommand pc && seen.add(command)) {
                byPlugin.computeIfAbsent(pc.getPlugin(), k -> new ArrayList<>()).add(command);
            }
        }

        List<PluginDigest.Entry> entries = new ArrayList<>();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (!p.isEnabled()) {
                continue;
            }
            String name = p.getPluginMeta().getName();
            if (s.pluginsExclude.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            List<PluginDigest.Cmd> commands = new ArrayList<>();
            for (Command c : byPlugin.getOrDefault(p, List.of())) {
                commands.add(new PluginDigest.Cmd(c.getName(), c.getUsage(), c.getDescription(),
                        List.copyOf(c.getAliases()), isPublic(c)));
            }
            entries.add(new PluginDigest.Entry(name, p.getPluginMeta().getVersion(),
                    p.getPluginMeta().getDescription(), commands));
        }
        return entries;
    }

    /**
     * Lệnh "công khai" = không cần quyền, hoặc mọi quyền cần thiết đều mặc định cấp cho tất cả người chơi.
     * Đây chỉ là ước lượng (plugin phân quyền như LuckPerms có thể cấp thêm), và luôn thiên về phía kín:
     * quyền chưa đăng ký hoặc mặc định cho OP thì coi là không công khai.
     */
    private static boolean isPublic(Command command) {
        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) {
            return true;
        }
        for (String part : permission.split(";")) {
            Permission registered = Bukkit.getPluginManager().getPermission(part.trim());
            if (registered == null || registered.getDefault() != PermissionDefault.TRUE) {
                return false;
            }
        }
        return true;
    }
}
