package com.glisco.numismaticoverhaul.item;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.currency.CurrencyResolver;
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

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.MONEY_BAG_COMPONENT;

public class MoneyBagItem extends Item implements CurrencyItem {

    public MoneyBagItem(ResourceKey<Item> key) {
        super(new Item.Properties().stacksTo(1).component(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)).setId(key));
    }

    public static ItemStack create(ItemStack firstStack, ItemStack otherStack) {
        var stack = new ItemStack(NumismaticOverhaulItems.MONEY_BAG);
        if (firstStack.has(MONEY_BAG_COMPONENT) && otherStack.has(MONEY_BAG_COMPONENT)) {
            stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.combine(firstStack, otherStack));
        } else if (firstStack.getItem() instanceof CurrencyItem coins && otherStack.getItem() instanceof CurrencyItem coins2) {
            var values1 = coins.getCombinedValue(firstStack);
            var values2 = coins2.getCombinedValue(otherStack);
            stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.combine(values1, values2));
        }
        return stack;
    }

    public static ItemStack fromValues(long[] values) {
        var stack = new ItemStack(NumismaticOverhaulItems.MONEY_BAG);
        stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.of(values));
        return stack;
    }

    public static ItemStack fromRawValue(long value) {
        var stack = new ItemStack(NumismaticOverhaulItems.MONEY_BAG);
        stack.set(MONEY_BAG_COMPONENT, MoneyBagComponent.of(value));
        return stack;
    }

    public long getValue(ItemStack stack) {
        return stack.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)).value();
    }

    @Override
    public long[] getCombinedValue(ItemStack stack) {
        var bagComponent = stack.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0));
        return new long[]{bagComponent.bronze(), bagComponent.silver(), bagComponent.gold()};
    }

    public boolean onClicked(ItemStack clickedStack, ItemStack otherStack, Slot slot, ContainerInput clickType, Player player, ItemContainerContents cursorStackReference) {
        // TODO: handle trade slots if needed

        // Withdraw from money bag
        if (clickedStack.getItem() == this && otherStack.isEmpty()) {
            var coins = getCombinedValue(clickedStack);
            final var stackRepresentation = CurrencyConverter.getAsValidStacks(coins);
            if (stackRepresentation.isEmpty()) return false;

            final var coinStack = stackRepresentation.getFirst();
            if (!player.getInventory().add(coinStack.copy())) return false;

            final long[] values = getCombinedValue(clickedStack);
            values[((CoinItem) coinStack.getItem()).currency.ordinal()] -= coinStack.getCount();

            final long newValue = CurrencyResolver.combineValues(values);
            final boolean canBeCompacted = CurrencyResolver.canBeCompacted(values);

            if (newValue == 0) {
                slot.set(ItemStack.EMPTY);
            } else if (canBeCompacted && CurrencyConverter.getAsValidStacks(newValue).size() == 1) {
                slot.set(CurrencyConverter.getAsValidStacks(newValue).getFirst());
            } else {
                slot.set(fromValues(values));
            }

        } else if (clickType == ContainerInput.PICKUP) {
            if (!(otherStack.getItem() instanceof CurrencyItem currencyItem)) return false;
            final var bag = MoneyBagItem.create(clickedStack, otherStack);
            if (bag.getOrDefault(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)).value() == 0) return false;
            if (!slot.mayPlace(bag)) return false;

            slot.set(bag);
            return false;
        }

        return true;
    }

    public Optional<TooltipProvider> getTooltipProvider(ItemStack stack) {
        var values = this.getCombinedValue(stack);
        return Optional.of(new CurrencyTooltipProvider(values, new long[]{-1}));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ModComponents.get(player).modify(getValue(player.getItemInHand(hand)));
        player.setItemInHand(hand, ItemStack.EMPTY);
        return InteractionResult.SUCCESS;
    }

    public boolean wasAdjusted(ItemStack other) {
        return true;
    }

    public Component getHoverName() {
        return new ItemStack(this).getHoverName().copy().setStyle(NumismaticOverhaulItems.SILVER_COIN.NAME_STYLE);
    }
}
