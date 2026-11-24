package com.astrax.ai;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** AstraxAI - trợ lý AI cho server Paper 1.21.1. */
public final class AstraxAI extends JavaPlugin {

    private volatile Settings settings;
    private volatile AiClient client;

    private ExecutorService executor;
    private KnowledgeBase knowledge;
    private ChatMemory memory;
    private ChatInputManager chatInput;
    private WarningTracker warnings;
    private ServerMonitor monitor;
    private ChatService chatService;
    private AdminService adminService;
    private MainMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(getConfig());

        this.executor = createExecutor(settings.threads);
        this.client = new AiClient(settings, executor);

        this.warnings = new WarningTracker();
        this.memory = new ChatMemory(settings.memoryEnabled, settings.maxHistory);
        this.knowledge = new KnowledgeBase(this);
        this.knowledge.load(settings.knowledgeMaxChars);
        this.chatInput = new ChatInputManager();
        this.monitor = new ServerMonitor(this);
        this.chatService = new ChatService(this);
        this.adminService = new AdminService(this);
        this.menu = new MainMenu(this);

        registerCommand("ai", new AiCommand(this));
        registerCommand("astraai", new AdminCommand(this));

        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);

        // Chỉ ghi trạng thái, tuyệt đối không ghi nội dung API Key.
        getLogger().info("AstraxAI đã bật. Model: " + settings.model
                + " | API Key: " + (settings.hasApiKey() ? "đã cấu hình" : "CHƯA cấu hình"));
        if (!settings.hasApiKey()) {
            getLogger().warning("Chưa có API Key. Hãy điền api.key trong plugins/AstraxAI/config.yml "
                    + "(hoặc đặt biến môi trường ASTRAXAI_API_KEY) rồi chạy /astraai reload.");
        }
        if (!settings.isUrlSecure()) {
            getLogger().warning("api.url không bắt đầu bằng https:// nên sẽ bị từ chối. Hãy sửa lại config.yml.");
        }
    }

    @Override
    public void onDisable() {
        // Đóng menu đang mở để người chơi không giữ inventory của plugin đã tắt.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MainMenu.Holder) {
                player.closeInventory();
            }
        }

        if (client != null) {
            client.close();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (memory != null) {
            memory.clearAll();
        }
        if (chatInput != null) {
            chatInput.clear();
        }
    }

    /**
     * Nạp lại config.yml và knowledge.yml.
     *
     * @return false nếu knowledge.yml bị lỗi
     */
    public boolean reloadAll() {
        reloadConfig();
        Settings fresh = new Settings(getConfig());
        this.settings = fresh;
        this.client = new AiClient(fresh, executor);
        this.memory.configure(fresh.memoryEnabled, fresh.maxHistory);
        return knowledge.load(fresh.knowledgeMaxChars);
    }

    private void registerCommand(String name, TabExecutor handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Không tìm thấy lệnh '" + name + "' trong plugin.yml.");
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private static ExecutorService createExecutor(int threads) {
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "AstraxAI-Worker-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(threads, factory);
    }

    // ---------------------------------------------------------------- accessors

    public Settings settings() {
        return settings;
    }

    public AiClient client() {
        return client;
    }

    public ExecutorService executor() {
        return executor;
    }

    public KnowledgeBase knowledge() {
        return knowledge;
    }

    public ChatMemory memory() {
        return memory;
    }

    public ChatInputManager chatInput() {
        return chatInput;
    }

    public WarningTracker warnings() {
        return warnings;
    }

    public ServerMonitor monitor() {
        return monitor;
    }

    public ChatService chatService() {
        return chatService;
    }

    public AdminService adminService() {
        return adminService;
    }

    public MainMenu menu() {
        return menu;
    }
}
