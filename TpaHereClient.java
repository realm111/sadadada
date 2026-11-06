package pl.tpahere.auto;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TpaHereClient implements ClientModInitializer {
    public static final Config CONFIG = Config.load();
    public static int sentCount = 0;

    private static KeyBinding openKey;
    private static final Map<String, Long> lastSent = new HashMap<>();
    private static final ArrayDeque<String> queue = new ArrayDeque<>();
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tpahere.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(Identifier.of("tpahere", "main"))));

        // Czat podpisany (vanilla / wiekszosc serwerow)
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> {
            if (sender != null) {
                onPlayerChatted(sender.name());
            } else {
                parseSystemMessage(message.getString());
            }
        });

        // Czat jako wiadomosc systemowa (Paper/Spigot z pluginami od formatowania czatu)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) parseSystemMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new TpaHereScreen());
                }
            }
            processQueue(client);
        });

        // Wyczysc stan po wyjsciu z serwera
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearState());
    }

    public static void clearState() {
        queue.clear();
        lastSent.clear();
    }

    private static void processQueue(MinecraftClient client) {
        if (tickCounter < 10000) tickCounter++;
        if (!CONFIG.enabled || queue.isEmpty()) return;
        if (client.player == null || client.getNetworkHandler() == null) return;
        if (tickCounter < CONFIG.delayTicks) return;

        String name = queue.poll();
        String cmd = CONFIG.command.replace("{player}", name);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        client.getNetworkHandler().sendChatCommand(cmd);
        sentCount++;
        tickCounter = 0;
    }

    private static void onPlayerChatted(String name) {
        if (!CONFIG.enabled || name == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getGameProfile().name().equalsIgnoreCase(name)) return;

        long now = System.currentTimeMillis();
        String key = name.toLowerCase();
        Long last = lastSent.get(key);
        if (last != null && now - last < CONFIG.cooldownSeconds * 1000L) return;
        lastSent.put(key, now);
        if (!queue.contains(name)) queue.add(name);
    }

    /**
     * Wiadomosci systemowe: szukamy nicka online na poczatku wiadomosci,
     * po ktorym jest separator czatu (: » > ›). Dzieki temu join/leave/death nie sa liczone.
     */
    private static void parseSystemMessage(String text) {
        if (!CONFIG.enabled || text == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        String bestName = null;
        int bestIdx = Integer.MAX_VALUE;
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().name();
            Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(n) + "(?![A-Za-z0-9_])").matcher(text);
            if (m.find() && m.start() < 40 && m.start() < bestIdx) {
                String after = text.substring(m.end());
                if (after.contains(":") || after.contains("»") || after.contains(">") || after.contains("›")) {
                    bestIdx = m.start();
                    bestName = n;
                }
            }
        }
        if (bestName != null) onPlayerChatted(bestName);
    }
}
