package com.camjewell;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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

    @ConfigItem(keyName = "mappingLines", name = "Color Remappings", description = "One mapping per line in format: RRGGBB=RRGGBB (example: FF0000=00FF00)")
    default String mappingLines() {
        return "";
    }

    @ConfigItem(keyName = "captureOriginalEnabled", name = "Capture Original Color", description = "Enable, then click an existing chat message to capture its color into Last Captured Original")
    default boolean captureOriginalEnabled() {
        return false;
    }

    @ConfigItem(keyName = "lastCapturedOriginal", name = "Last Captured Original", description = "Auto-filled when Capture Original Color is enabled and you click an existing colored chat message")
    default String lastCapturedOriginal() {
        return "";
    }

    @ConfigItem(keyName = "captureNewEnabled", name = "Capture New Color", description = "Enable, then click an existing chat message to capture its color into Last Captured New")
    default boolean captureNewEnabled() {
        return false;
    }

    @ConfigItem(keyName = "lastCapturedNew", name = "Last Captured New", description = "Auto-filled when Capture New Color is enabled and you click an existing colored chat message")
    default String lastCapturedNew() {
        return "";
    }

    @ConfigItem(keyName = "debugMode", name = "Debug Mode", description = "Log chat message interception details to console")
    default boolean debugMode() {
        return false;
    }
}
