package dev.wynnrolls.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public class ServerDetector {

    private static boolean onWynncraft = false;

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            onWynncraft = checkIfWynncraft(client);
            if (onWynncraft) {
                DebugLogger.log("Wynncraft server detected — mod is active.");
            } else {
                DebugLogger.log("Not on Wynncraft — mod is inactive.");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onWynncraft = false;
            DebugLogger.log("Disconnected — mod reset.");
        });
    }

    private static boolean checkIfWynncraft(MinecraftClient client) {
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) {
            // In singleplayer or integrated server
            return false;
        }
        String address = serverInfo.address.toLowerCase();
        return address.contains("wynncraft") || address.contains("wynncraft.com");
    }

    public static boolean isOnWynncraft() {
        return onWynncraft;
    }

    /**
     * Force-enable for testing without being on the actual server.
     */
    public static void forceEnable() {
        onWynncraft = true;
        DebugLogger.log("FORCE ENABLED (dev mode) — mod active regardless of server.");
    }
}
