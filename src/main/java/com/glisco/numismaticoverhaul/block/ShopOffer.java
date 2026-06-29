package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.item.MoneyBagComponent;
import com.glisco.numismaticoverhaul.item.MoneyBagItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;

public record ShopOffer(ItemStack sell, long price) {
    public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("sell").forGetter(ShopOffer::sell),
        Codec.LONG.fieldOf("price").forGetter(ShopOffer::price)
    ).apply(instance, ShopOffer::new));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ShopOffer> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, ShopOffer::sell,
        ByteBufCodecs.VAR_LONG, ShopOffer::price,
        ShopOffer::new
    );


    public ShopOffer {
        if (sell.isEmpty()) throw new IllegalArgumentException("Sell Stack must not be empty");
        if (price == 0) throw new IllegalArgumentException("Price must not be null");
    }

    public MerchantOffer toTradeOffer(ShopBlockEntity shop, boolean inexhaustible) {
        boolean isPocketChange = CurrencyConverter.getRequiredCurrencyTypes(price) == 1;
        var buyStack = isPocketChange ? CurrencyConverter.getAsItemStackList(price).getFirst() : MoneyBagItem.fromRawValue(price);
        int maxUses = inexhaustible ? Integer.MAX_VALUE : count(shop.getItems(), sell) / sell.getCount();
        var tradedItem = new ItemCost(buyStack.getItem(), buyStack.getCount());

        return new MerchantOffer(tradedItem, sell, maxUses, 0, 0);
    }

    public long getPrice() {
        return price;
    }

    public ItemStack getSellStack() {
        return sell.copy();
    }

    public static int count(NonNullList<ItemStack> stacks, ItemStack testStack) {
        int count = 0;
        for (var stack : stacks) {
            if (!ItemStack.isSameItemSameComponents(stack, testStack)) continue;
            count += stack.getCount();
        }
        return count;
    }

    public static int remove(NonNullList<ItemStack> stacks, ItemStack removeStack) {
        int toRemove = removeStack.getCount();
        for (var stack : stacks) {
            if (!ItemStack.isSameItemSameComponents(stack, removeStack)) continue;

            int removed = stack.getCount();
            stack.shrink(toRemove);

            toRemove -= removed;
            if (toRemove < 1) break;
        }
        return removeStack.getCount() - toRemove;
    }

    @Override
    public String toString() {
        return this.sell + "@" + this.price + "coins";
    }
}
