package com.astrax.ai;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /ai <tin nhắn> | /ai clear | /ai help | /ai menu */
public final class AiCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("help", "clear", "menu");

    private final AstraxAI plugin;

    public AiCommand(AstraxAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "&cLệnh này chỉ dành cho người chơi. Quản trị viên dùng &f/astraai&c.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "help" -> {
                    sendHelp(player);
                    return true;
                }
                case "clear" -> {
                    plugin.chatService().clearHistory(player);
                    return true;
                }
                case "menu" -> {
                    if (!player.hasPermission("astraxai.gui")) {
                        Text.send(player, "&cBạn không có quyền mở menu AstraxAI.");
                    } else {
                        plugin.menu().open(player);
                    }
                    return true;
                }
                default -> {
                    // Không phải lệnh con: xử lý như một câu hỏi.
                }
            }
        }

        plugin.chatService().ask(player, String.join(" ", args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    result.add(sub);
                }
            }
        }
        return result;
    }

    private void sendHelp(Player player) {
        Text.sendRaw(player, "&b===== AstraxAI - Trợ lý AI =====");
        Text.sendRaw(player, "&e/ai <tin nhắn> &7- Trò chuyện với trợ lý AI");
        Text.sendRaw(player, "&e/ai clear &7- Xóa lịch sử trò chuyện của bạn");
        Text.sendRaw(player, "&e/ai menu &7- Mở menu AI");
        Text.sendRaw(player, "&e/ai help &7- Xem hướng dẫn này");
        Text.sendRaw(player, "&7AI trả lời dựa trên thông tin do admin cấu hình cho server.");
        Text.sendRaw(player, "&7Đừng chia sẻ mật khẩu hay thông tin cá nhân với AI.");
    }
}
