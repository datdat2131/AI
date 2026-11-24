package com.astrax.ai.command;

import com.astrax.ai.AstraxAI;
import com.astrax.ai.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /astraai <status | reload | diagnose | report> (alias: /astraxai) */
public final class AdminCommand implements TabExecutor {

    private final AstraxAI plugin;

    public AdminCommand(AstraxAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> plugin.adminService().status(sender);
            case "diagnose" -> plugin.adminService().diagnose(sender);
            case "report" -> plugin.adminService().report(sender);
            case "reload" -> reload(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("astraxai.reload")) {
            Text.send(sender, "&cBạn không có quyền reload AstraxAI.");
            return;
        }
        boolean knowledgeOk = plugin.reloadAll();
        if (knowledgeOk) {
            Text.send(sender, "&aĐã reload config.yml, knowledge.yml và shop.yml.");
        } else {
            Text.send(sender, "&eĐã reload config.yml nhưng knowledge.yml hoặc shop.yml bị lỗi, hãy xem console.");
        }
        if (!plugin.settings().hasApiKey()) {
            Text.send(sender, "&eLưu ý: chưa cấu hình API Key nên AI chưa hoạt động.");
        }
        if (!plugin.settings().isUrlSecure()) {
            Text.send(sender, "&eLưu ý: api.url phải bắt đầu bằng https://");
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        if (!sender.hasPermission("astraxai.admin") && !sender.hasPermission("astraxai.reload")) {
            Text.send(sender, "&cBạn không có quyền sử dụng lệnh này.");
            return;
        }
        Text.sendRaw(sender, "&b===== AstraxAI - Quản trị =====");
        if (sender.hasPermission("astraxai.admin")) {
            Text.sendRaw(sender, "&e/" + label + " status &7- Xem TPS, MSPT, RAM, người chơi, plugin");
            Text.sendRaw(sender, "&e/" + label + " diagnose &7- AI chẩn đoán nhanh tình trạng server");
            Text.sendRaw(sender, "&e/" + label + " report &7- AI tạo báo cáo đầy đủ và lưu thành file");
        }
        if (sender.hasPermission("astraxai.reload")) {
            Text.sendRaw(sender, "&e/" + label + " reload &7- Nạp lại config.yml và knowledge.yml");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length != 1) {
            return result;
        }
        List<String> options = new ArrayList<>();
        if (sender.hasPermission("astraxai.admin")) {
            options.add("status");
            options.add("diagnose");
            options.add("report");
        }
        if (sender.hasPermission("astraxai.reload")) {
            options.add("reload");
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
