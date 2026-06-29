package com.glisco.numismaticoverhaul.item;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.Currency;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.inventory.Slot;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import java.util.Optional;

public class CoinItem extends Item implements CurrencyItem {

    public final Currency currency;
    public final net.minecraft.network.chat.Style NAME_STYLE;

    public CoinItem(ResourceKey<Item> key, Currency currency) {
        super(new Item.Properties().stacksTo(99).setId(key));
        this.currency = currency;
        this.NAME_STYLE = net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(currency.getNameColor()));
    }

    public boolean onClicked(ItemStack clickedStack, ItemStack cursorStack, Slot slot, ContainerInput clickType, Player player, ItemContainerContents cursorStackReference) {
        // TODO: handle trade slots if needed
        if (clickType != ContainerInput.PICKUP) return false;

        if ((cursorStack.getItem() == this && cursorStack.getCount() + clickedStack.getCount() <= cursorStack.getMaxStackSize()) || !(cursorStack.getItem() instanceof CurrencyItem currencyItem)) {
            return false;
        }

        final var stack = MoneyBagItem.create(clickedStack, cursorStack);
        if (!slot.mayPlace(stack)) return false;

        slot.set(stack);

        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack clickedStack = player.getItemInHand(hand);
        long rawValue = ((CoinItem) clickedStack.getItem()).currency.getRawValue(clickedStack.getCount());

        if (!level.isClientSide()) {
            ModComponents.get(player).modify(rawValue);
        }

        return InteractionResult.SUCCESS;
    }

    public Optional<TooltipProvider> getTooltipProvider(ItemStack stack) {
        return Optional.of(new CurrencyTooltipProvider(this.currency.getRawValue(stack.getCount()), -1));
    }

    public Component getDisplayName() {
        return new ItemStack(this).getHoverName().copy().setStyle(NAME_STYLE);
    }

    public Component getDisplayName(ItemStack stack) {
        return stack.getHoverName().copy().setStyle(NAME_STYLE);
    }

    public boolean wasAdjusted(ItemStack other) {
        return other.getItem() != this;
    }

    @Override
    public long getValue(ItemStack stack) {
        return this.currency.getRawValue(stack.getCount());
    }

    @Override
    public long[] getCombinedValue(ItemStack stack) {
        final long[] values = new long[3];
        values[this.currency.ordinal()] = stack.getCount();
        return values;
    }
}
