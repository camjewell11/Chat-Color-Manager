package com.camjewell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "Chat Color Manager", description = "Recolor chat messages for colorblind assistance", tags = {
        "chat", "color", "utility", "colorblind" })
public class ChatColorManagerPlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(ChatColorManagerPlugin.class);
    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<col=([0-9A-Fa-f]{6})>");

    @Inject
    private Client client;

    @Inject
    private ChatColorManagerConfig config;

    @Inject
    private ConfigManager configManager;
    private Map<String, String> colorRemapCache = new HashMap<>(); // For performance

    @Override
    protected void startUp() throws Exception {
        log.info("Chat Color Manager started");
        reloadMappingsFromConfig();
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Chat Color Manager stopped");
    }

    private void rebuildRemapCache(List<String> mappingLines) {
        colorRemapCache.clear();

        for (String line : mappingLines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] parts = trimmed.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String from = normalizeHex(parts[0]);
            String to = normalizeHex(parts[1]);
            if (from == null || to == null) {
                continue;
            }

            colorRemapCache.put(from, to);
        }

        if (config.debugMode()) {
            log.info("Rebuilt color remap cache with {} mappings", colorRemapCache.size());
        }
    }

    private void reloadMappingsFromConfig() {
        String raw = config.mappingLines();
        List<String> lines = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            String[] split = raw.split("\\r?\\n");
            for (String line : split) {
                lines.add(line);
            }
        }
        rebuildRemapCache(lines);
    }

    private String normalizeHex(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim().replace("#", "").toUpperCase();
        if (!cleaned.matches("[0-9A-F]{6}")) {
            return null;
        }
        return cleaned;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("chatcolormanager")) {
            if (event.getKey().equals("mappingLines")) {
                reloadMappingsFromConfig();
            }
            if (event.getKey().equals("captureOriginalEnabled") && config.captureOriginalEnabled()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "<col=66CCFF>[Chat Color Manager]</col> Capture Original enabled. Click an existing chat message to capture its color.",
                        null);
            }
            if (event.getKey().equals("captureNewEnabled") && config.captureNewEnabled()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "<col=66CCFF>[Chat Color Manager]</col> Capture New enabled. Click an existing chat message to capture its color.",
                        null);
            }
            if (config.debugMode()) {
                log.info("Chat Color Manager config changed: {}", event.getKey());
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (!config.enablePlugin()) {
            return;
        }

        String message = event.getMessage();

        if (config.debugMode()) {
            log.info("Intercepted chat message: {}", message);
        }

        // If color mappings are enabled, apply them
        if (config.useColorMappings() && !colorRemapCache.isEmpty()) {
            String remappedMessage = applyColorRemappings(message);
            event.setMessage(remappedMessage);

            if (config.debugMode()) {
                log.info("Applied color remappings: {}", remappedMessage);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        boolean captureOriginal = config.captureOriginalEnabled();
        boolean captureNew = config.captureNewEnabled();
        if (!captureOriginal && !captureNew) {
            return;
        }

        String target = event.getMenuTarget();
        String option = event.getMenuOption();
        String capturedHex = firstColorHex(target);
        if (capturedHex == null) {
            capturedHex = firstColorHex(option);
        }

        if (capturedHex == null) {
            return;
        }

        if (captureOriginal) {
            configManager.setConfiguration("chatcolormanager", "lastCapturedOriginal", capturedHex);
            configManager.setConfiguration("chatcolormanager", "captureOriginalEnabled", false);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "<col=66CCFF>[Chat Color Manager]</col> Captured #" + capturedHex
                            + " (saved to Last Captured Original).",
                    null);
        }

        if (captureNew) {
            configManager.setConfiguration("chatcolormanager", "lastCapturedNew", capturedHex);
            configManager.setConfiguration("chatcolormanager", "captureNewEnabled", false);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "<col=66CCFF>[Chat Color Manager]</col> Captured #" + capturedHex
                            + " (saved to Last Captured New).",
                    null);
        }

        if (config.debugMode()) {
            log.info("Captured chat color: {}", capturedHex);
        }
    }

    private String firstColorHex(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher matcher = COLOR_TAG_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).toUpperCase();
    }

    /**
     * Apply color remappings to a chat message.
     * Searches for color tags like <col=RRGGBB> and replaces them based on
     * mappings.
     */
    private String applyColorRemappings(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = COLOR_TAG_PATTERN.matcher(message);

        while (matcher.find()) {
            String originalColor = matcher.group(1).toUpperCase();
            String newColor = colorRemapCache.get(originalColor);

            if (newColor != null) {
                // Replace the color tag with the new color
                matcher.appendReplacement(result, "<col=" + newColor + ">");
            } else {
                // Keep the original color if no mapping exists
                matcher.appendReplacement(result, "<col=" + originalColor + ">");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
