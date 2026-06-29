package com.glisco.numismaticoverhaul.item;

import com.glisco.numismaticoverhaul.currency.CurrencyResolver;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.MONEY_BAG_COMPONENT;
import static com.glisco.numismaticoverhaul.NumismaticOverhaul.id;

public record MoneyBagComponent(long bronze, long silver, long gold) {

    public static final Codec<MoneyBagComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("bronze").forGetter(MoneyBagComponent::bronze),
        Codec.LONG.fieldOf("silver").forGetter(MoneyBagComponent::silver),
        Codec.LONG.fieldOf("gold").forGetter(MoneyBagComponent::gold)
    ).apply(instance, MoneyBagComponent::new));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, MoneyBagComponent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, MoneyBagComponent::bronze,
        ByteBufCodecs.VAR_LONG, MoneyBagComponent::silver,
        ByteBufCodecs.VAR_LONG, MoneyBagComponent::gold,
        MoneyBagComponent::new
    );

    public static MoneyBagComponent of(long[] values) {
        return MoneyBagComponent.of(
            values[0],
            values[1],
            values[2]
        );
    }

    public static MoneyBagComponent of(long value) {
        var money = CurrencyResolver.splitValues(value);
        return new MoneyBagComponent(money[0], money[1], money[2]);
    }

    public static MoneyBagComponent of(long bronze, long silver, long gold) {
        return new MoneyBagComponent(bronze, silver, gold);
    }

    public static MoneyBagComponent combine(long[] money, long[] money2) {
        return new MoneyBagComponent(
            money[0] + money2[0],
            money[1] + money2[1],
            money[2] + money2[2]
        );
    }

    public static MoneyBagComponent combine(ItemStack stack1, ItemStack stack2) {
        var values1 = stack1.getOrDefault(MONEY_BAG_COMPONENT, of(0));
        var values2 = stack2.getOrDefault(MONEY_BAG_COMPONENT, of(0));
        return new MoneyBagComponent(
            values1.bronze + values2.bronze,
            values1.silver + values2.silver,
            values1.gold + values2.gold
        );
    }

    public long value() {
        return CurrencyResolver.combineValues(bronze, silver, gold);
    }

    public static DataComponentType<MoneyBagComponent> register() {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("money_bag"), DataComponentType.<MoneyBagComponent>builder()
            .persistent(CODEC)
            .networkSynchronized(STREAM_CODEC)
            .build()
        );
    }
}
