package com.glisco.numismaticoverhaul.villagers.data;

import java.util.Map;

/**
 * Villager trades are now data-driven via JSON in MC 26.2.
 * This class is retained only for the getOrDefaultAndAdd utility.
 */
public class NumismaticVillagerTradesRegistry {

    public static <K, V> V getOrDefaultAndAdd(Map<K, V> map, K key, V defaultValue) {
        if (map.containsKey(key)) return map.get(key);
        map.put(key, defaultValue);
        return defaultValue;
    }
}
