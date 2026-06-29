package com.glisco.numismaticoverhaul.currency;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.glisco.numismaticoverhaul.item.MoneyBagItem;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;


import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
import java.util.function.Consumer;

public class MoneyBagLootEntry extends LootPoolSingletonContainer {
    public static final MapCodec<MoneyBagLootEntry> CODEC = MapCodec.unit(() -> new MoneyBagLootEntry(0, 0));

    private final int min;
    private final int max;

    protected MoneyBagLootEntry(int min, int max) {
        super(0, 0, List.of(), List.of());
        this.min = min;
        this.max = max;
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
        long amount = min + (long) (Math.random() * (max - min + 1));
        if (amount <= 0) return;
        consumer.accept(MoneyBagItem.fromRawValue(amount));
    }

    @Override
    public MapCodec<? extends LootPoolSingletonContainer> codec() {
        return CODEC;
    }

    public static LootPoolSingletonContainer.Builder<?> builder(int min, int max) {
        return simpleBuilder((weight, quality, conditions, functions) -> new MoneyBagLootEntry(min, max));
    }
}
