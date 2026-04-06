package com.camjewell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import com.google.inject.Provides;

@PluginDescriptor(name = "Chat Color Manager", description = "Recolor chat messages for colorblind assistance", tags = {
        "chat", "color", "utility", "colorblind" })
public class ChatColorManagerPlugin extends Plugin {

    private static final Logger log = LoggerFactory.getLogger(ChatColorManagerPlugin.class);
    private static final Pattern COLOR_TAG_PATTERN = Pattern.compile("<col=([0-9A-Fa-f]{6})>");
    private static final Pattern COLOR_TAG_NAMED_PATTERN = Pattern.compile("<col([A-Z_]+)>");

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChatColorManagerConfig config;

    @Inject
    private ConfigManager configManager;
    private Map<String, String> colorRemapCache = new HashMap<>(); // For performance

    @Provides
    ChatColorManagerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ChatColorManagerConfig.class);
    }

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

            String from = parts[0].trim().toUpperCase();
            String to = parts[1].trim().toUpperCase();
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }

            // Accept both hex (RRGGBB) and named (HIGHLIGHT) formats
            if (from.matches("[0-9A-F]{6}") && to.matches("[0-9A-F]{6}")) {
                // Both hex
                colorRemapCache.put(from, to);
            } else if (from.matches("[A-Z_]+") && to.matches("[A-Z_]+")) {
                // Both named colors
                colorRemapCache.put(from, to);
            }
            // Ignore mixed or invalid formats
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
            if (event.getKey().equals("addMapping") && config.addMapping()) {
                String fromHex = colorToHex(config.fromColor());
                String toHex = colorToHex(config.toColor());
                String newLine = fromHex + "=" + toHex;
                String existing = config.mappingLines();
                final String updated = (existing == null || existing.trim().isEmpty())
                        ? newLine
                        : existing.trim() + "\n" + newLine;
                final String msg = "<col=66CCFF>[Chat Color Manager]</col> Added mapping: #" + fromHex + " \u2192 #"
                        + toHex;
                SwingUtilities.invokeLater(() -> {
                    configManager.setConfiguration("chatcolormanager", "mappingLines", updated);
                    configManager.setConfiguration("chatcolormanager", "addMapping", false);
                });
                clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null));
            }
            if (config.debugMode()) {
                log.info("Chat Color Manager config changed: {}", event.getKey());
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        log.info("[CCM] onChatMessage fired: type={} msg={}", event.getType(), event.getMessage());

        String message = event.getMessage();

        // Append color codes regardless of enablePlugin so users can identify colors
        if (config.showColorCodes()) {
            List<String> foundColors = allColorHexes(message);
            if (!foundColors.isEmpty()) {
                StringBuilder suffix = new StringBuilder(" <col=AAAAAA>[");
                for (int i = 0; i < foundColors.size(); i++) {
                    if (i > 0)
                        suffix.append(", ");
                    suffix.append("#").append(foundColors.get(i));
                }
                suffix.append("]</col>");
                event.setMessage(message + suffix);
            } else {
                event.setMessage(message + " <col=AAAAAA>[no color tags]</col>");
            }
            return;
        }

        if (!config.enablePlugin()) {
            return;
        }

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

    private List<String> allColorHexes(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        // Hex colors: <col=RRGGBB>
        Matcher hexMatcher = COLOR_TAG_PATTERN.matcher(text);
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1).toUpperCase();
            if (!result.contains(hex)) {
                result.add(hex);
            }
        }
        // Named colors: <colHIGHLIGHT>, <colWARN>, etc.
        Matcher namedMatcher = COLOR_TAG_NAMED_PATTERN.matcher(text);
        while (namedMatcher.find()) {
            String named = namedMatcher.group(1);
            if (!result.contains(named)) {
                result.add(named);
            }
        }
        return result;
    }

    private String colorToHex(Color color) {
        return String.format("%06X", color.getRGB() & 0xFFFFFF);
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

        // Replace hex colors: <col=RRGGBB>
        Matcher hexMatcher = COLOR_TAG_PATTERN.matcher(message);
        while (hexMatcher.find()) {
            String originalColor = hexMatcher.group(1).toUpperCase();
            String newColor = colorRemapCache.get(originalColor);
            if (newColor != null && newColor.matches("[0-9A-F]{6}")) {
                hexMatcher.appendReplacement(result, "<col=" + newColor + ">");
            } else {
                hexMatcher.appendReplacement(result, "<col=" + originalColor + ">");
            }
        }
        hexMatcher.appendTail(result);
        message = result.toString();

        // Replace named colors: <colHIGHLIGHT>
        StringBuffer result2 = new StringBuffer();
        Matcher namedMatcher = COLOR_TAG_NAMED_PATTERN.matcher(message);
        while (namedMatcher.find()) {
            String originalColor = namedMatcher.group(1).toUpperCase();
            String newColor = colorRemapCache.get(originalColor);
            if (newColor != null && newColor.matches("[A-Z_]+")) {
                namedMatcher.appendReplacement(result2, "<col" + newColor + ">");
            } else {
                namedMatcher.appendReplacement(result2, "<col" + originalColor + ">");
            }
        }
        namedMatcher.appendTail(result2);

        return result2.toString();
    }

}
