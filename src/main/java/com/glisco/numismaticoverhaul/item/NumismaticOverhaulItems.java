package com.glisco.numismaticoverhaul.item;

import com.glisco.numismaticoverhaul.currency.Currency;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class NumismaticOverhaulItems {
    public static CoinItem BRONZE_COIN;
    public static CoinItem SILVER_COIN;
    public static CoinItem GOLD_COIN;
    public static MoneyBagItem MONEY_BAG;

    public static void register() {
        BRONZE_COIN = new CoinItem(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("bronze_coin")), Currency.BRONZE);
        SILVER_COIN = new CoinItem(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("silver_coin")), Currency.SILVER);
        GOLD_COIN = new CoinItem(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("gold_coin")), Currency.GOLD);
        MONEY_BAG = new MoneyBagItem(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("money_bag")));

        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("bronze_coin"), BRONZE_COIN);
        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("silver_coin"), SILVER_COIN);
        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("gold_coin"), GOLD_COIN);
        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("money_bag"), MONEY_BAG);
    }
}

