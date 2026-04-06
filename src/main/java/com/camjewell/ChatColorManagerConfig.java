package com.camjewell;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("chatcolormanager")
public interface ChatColorManagerConfig extends Config {

    @ConfigItem(keyName = "enablePlugin", name = "Enable Chat Recoloring", description = "Enable or disable chat message recoloring")
    default boolean enablePlugin() {
        return true;
    }

    @ConfigItem(keyName = "useColorMappings", name = "Use Color Mappings", description = "Enable/disable custom color mappings (colorblind assistance)")
    default boolean useColorMappings() {
        return true;
    }

    @ConfigItem(keyName = "mappingLines", name = "Color Remappings", description = "One mapping per line. Hex format: RRGGBB=RRGGBB (e.g., FF0000=00FF00). Named colors: COLORNAME=COLORNAME (e.g., HIGHLIGHT=WARN)")
    default String mappingLines() {
        return "";
    }

    @ConfigSection(name = "Add New Mapping", description = "Pick two colors using the eyedropper, then toggle Add Mapping to save", position = 10)
    String addMappingSection = "addMappingSection";

    @ConfigItem(keyName = "fromColor", name = "From Color", description = "Use the eyedropper to pick the original color from a chat message", section = "addMappingSection")
    default Color fromColor() {
        return Color.RED;
    }

    @ConfigItem(keyName = "toColor", name = "To Color", description = "Use the eyedropper to pick the replacement color", section = "addMappingSection")
    default Color toColor() {
        return Color.GREEN;
    }

    @ConfigItem(keyName = "addMapping", name = "Add Mapping", description = "Toggle on to append the From/To colors above as a new mapping", section = "addMappingSection")
    default boolean addMapping() {
        return false;
    }

    @ConfigItem(keyName = "showColorCodes", name = "Show Color Codes in Chat", description = "Appends the hex color code(s) found in each chat message so you can identify colors to remap")
    default boolean showColorCodes() {
        return false;
    }

    @ConfigItem(keyName = "debugMode", name = "Debug Mode", description = "Log chat message interception details to console")
    default boolean debugMode() {
        return false;
    }
}
