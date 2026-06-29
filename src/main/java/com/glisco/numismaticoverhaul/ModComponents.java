package com.glisco.numismaticoverhaul;

import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import net.minecraft.world.entity.player.Player;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModComponents {

    private static final Map<UUID, long[]> CURRENCY_STORAGE = new ConcurrentHashMap<>();
    private static Path dataDirectory;

    public static void init(Path worldDir) {
        dataDirectory = worldDir.resolve("numismatic-overhaul");
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            NumismaticOverhaul.LOGGER.error("Failed to create data directory", e);
        }
        loadAll();
    }

    public static CurrencyComponent get(Player player) {
        return new CurrencyComponent(player);
    }

    public static long[] getCurrencyData(UUID uuid) {
        return CURRENCY_STORAGE.getOrDefault(uuid, new long[]{0, 0, 0});
    }

    public static void setCurrencyData(UUID uuid, long[] data) {
        CURRENCY_STORAGE.put(uuid, data);
    }

    public static void copyOnRespawn(Player oldPlayer, Player newPlayer) {
        long[] data = CURRENCY_STORAGE.remove(oldPlayer.getUUID());
        if (data != null) {
            CURRENCY_STORAGE.put(newPlayer.getUUID(), data);
        }
    }

    public static void loadAll() {
        if (dataDirectory == null) return;
        Path saveFile = dataDirectory.resolve("currency.dat");
        if (!Files.exists(saveFile)) return;

        try (DataInputStream dis = new DataInputStream(Files.newInputStream(saveFile))) {
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                UUID uuid = new UUID(dis.readLong(), dis.readLong());
                long[] values = new long[3];
                for (int j = 0; j < 3; j++) {
                    values[j] = dis.readLong();
                }
                CURRENCY_STORAGE.put(uuid, values);
            }
        } catch (IOException e) {
            NumismaticOverhaul.LOGGER.error("Failed to load currency data", e);
        }
    }

    public static void saveAll() {
        if (dataDirectory == null) return;
        Path saveFile = dataDirectory.resolve("currency.dat");

        try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(saveFile))) {
            dos.writeInt(CURRENCY_STORAGE.size());
            for (var entry : CURRENCY_STORAGE.entrySet()) {
                dos.writeLong(entry.getKey().getMostSignificantBits());
                dos.writeLong(entry.getKey().getLeastSignificantBits());
                for (long v : entry.getValue()) {
                    dos.writeLong(v);
                }
            }
        } catch (IOException e) {
            NumismaticOverhaul.LOGGER.error("Failed to save currency data", e);
        }
    }
}
