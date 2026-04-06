package com.camjewell;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChatColorManagerPluginLauncher {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(ChatColorManagerPlugin.class);
        RuneLite.main(args);
    }
}
