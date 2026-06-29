package com.glisco.numismaticoverhaul;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class NumismaticOverhaulConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("numismatic-overhaul.json");

    // Fields with defaults
    private boolean enableVillagerTrading = true;
    private boolean generateCurrencyInChests = true;
    private MoneyMessageLocation moneyMessageLocation = MoneyMessageLocation.ACTIONBAR;
    private PurseOffsets purseOffsets = new PurseOffsets();
    private LootOptions lootOptions = new LootOptions();
    private Map<String, Integer> mobsToBaseValues = new HashMap<>(Map.of("#numismatic-overhaul:the_bourgeoisie", 75));
    private boolean scaleOnHealth = false;
    private float healthScaleReduction = 1.0f;

    // Listener support for mobsToBaseValues
    private transient List<Consumer<Map<String, Integer>>> mobValueListeners = new ArrayList<>();

    // Public accessors matching generated wrapper API
    public boolean enableVillagerTrading() { return enableVillagerTrading; }
    public boolean generateCurrencyInChests() { return generateCurrencyInChests; }
    public MoneyMessageLocation moneyMessageLocation() { return moneyMessageLocation; }
    public PurseOffsets purseOffsets() { return purseOffsets; }
    public LootOptions lootOptions() { return lootOptions; }
    public Map<String, Integer> mobsToBaseValues() { return mobsToBaseValues; }
    public boolean scaleOnHealth() { return scaleOnHealth; }
    public float healthScaleReduction() { return healthScaleReduction; }

    public void subscribeToMobsToBaseValues(Consumer<Map<String, Integer>> listener) {
        mobValueListeners.add(listener);
    }

    // Nested static classes with accessor methods
    public static class PurseOffsets {
        public int survivalX = 0;
        public int survivalY = 0;
        public int creativeX = 0;
        public int creativeY = 0;
        public int merchantX = 0;
        public int merchantY = 0;

        public int survivalX() { return survivalX; }
        public int survivalY() { return survivalY; }
        public int creativeX() { return creativeX; }
        public int creativeY() { return creativeY; }
        public int merchantX() { return merchantX; }
        public int merchantY() { return merchantY; }
    }

    public static class LootOptions {
        public int desertMinLoot = 300;
        public int desertMaxLoot = 1200;
        public int dungeonMinLoot = 500;
        public int dungeonMaxLoot = 2000;
        public int structureMinLoot = 1500;
        public int structureMaxLoot = 4000;
        public int strongholdLibraryMinLoot = 2000;
        public int strongholdLibraryMaxLoot = 6000;

        public int desertMinLoot() { return desertMinLoot; }
        public int desertMaxLoot() { return desertMaxLoot; }
        public int dungeonMinLoot() { return dungeonMinLoot; }
        public int dungeonMaxLoot() { return dungeonMaxLoot; }
        public int structureMinLoot() { return structureMinLoot; }
        public int structureMaxLoot() { return structureMaxLoot; }
        public int strongholdLibraryMinLoot() { return strongholdLibraryMinLoot; }
        public int strongholdLibraryMaxLoot() { return strongholdLibraryMaxLoot; }
    }

    // Factory method
    public static NumismaticOverhaulConfig createAndLoad() {
        NumismaticOverhaulConfig config = new NumismaticOverhaulConfig();

        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                NumismaticOverhaulConfig loaded = GSON.fromJson(reader, NumismaticOverhaulConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException e) {
                NumismaticOverhaul.LOGGER.error("Failed to load numismatic-overhaul config", e);
            }
        }

        // Ensure nested objects are not null
        if (config.purseOffsets == null) config.purseOffsets = new PurseOffsets();
        if (config.lootOptions == null) config.lootOptions = new LootOptions();
        if (config.mobsToBaseValues == null) config.mobsToBaseValues = new HashMap<>(Map.of("#numismatic-overhaul:the_bourgeoisie", 75));
        // listeners list already initialized via field declaration

        // Write back to file to ensure all fields exist
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            NumismaticOverhaul.LOGGER.error("Failed to save numismatic-overhaul config", e);
        }
    }

    public void reload() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                NumismaticOverhaulConfig loaded = GSON.fromJson(reader, NumismaticOverhaulConfig.class);
                if (loaded != null) {
                    this.enableVillagerTrading = loaded.enableVillagerTrading;
                    this.generateCurrencyInChests = loaded.generateCurrencyInChests;
                    this.moneyMessageLocation = loaded.moneyMessageLocation;
                    this.purseOffsets = loaded.purseOffsets != null ? loaded.purseOffsets : new PurseOffsets();
                    this.lootOptions = loaded.lootOptions != null ? loaded.lootOptions : new LootOptions();
                    this.mobsToBaseValues = loaded.mobsToBaseValues != null ? loaded.mobsToBaseValues : new HashMap<>(Map.of("#numismatic-overhaul:the_bourgeoisie", 75));
                    this.scaleOnHealth = loaded.scaleOnHealth;
                    this.healthScaleReduction = loaded.healthScaleReduction;
                }
            } catch (IOException e) {
                NumismaticOverhaul.LOGGER.error("Failed to reload numismatic-overhaul config", e);
            }
        }
        // Notify listeners
        for (Consumer<Map<String, Integer>> listener : mobValueListeners) {
            try {
                listener.accept(mobsToBaseValues);
            } catch (Exception e) {
                NumismaticOverhaul.LOGGER.error("Error in mob value config listener", e);
            }
        }
    }
}
