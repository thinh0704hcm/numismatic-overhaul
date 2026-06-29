package com.glisco.numismaticoverhaul.villagers.json;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Villager trades are now data-driven via JSON in MC 26.2.
 * This class retains no-op stubs for API compatibility.
 */
public class VillagerTradesHandler {

    public static final Map<String, Integer> professionKeys = new HashMap<>();

    static {
        professionKeys.put("novice", 1);
        professionKeys.put("apprentice", 2);
        professionKeys.put("journeyman", 3);
        professionKeys.put("expert", 4);
        professionKeys.put("master", 5);
    }

    public static void registerDefaultAdapters() {
        // No-op — trades are now data-driven via MC 26.2 resource packs
    }

    public static void broadcastErrors(MinecraftServer server) {
        // No-op — no longer tracking deserialization errors
    }

    public static void broadcastErrors(List<ServerPlayer> players) {
        // No-op
    }
}
