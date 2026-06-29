package com.glisco.numismaticoverhaul.item;

import com.glisco.numismaticoverhaul.currency.CurrencyResolver;
import net.minecraft.world.item.component.TooltipProvider;

public final class CurrencyTooltipProvider implements TooltipProvider {

    private final long[] value;
    private final long[] original;

    // TODO - Working with raw long arrays sucks. Migrate this to use CurrencyComponent or similar
    public CurrencyTooltipProvider(long[] value, long[] original) {
        this.value = value;
        this.original = original;
    }

    public CurrencyTooltipProvider(long value, long original) {
        this.value = CurrencyResolver.splitValues(value);
        if (original == -1) {
            this.original = new long[]{-1};
        } else {
            this.original = CurrencyResolver.splitValues(original);
        }
    }

    public long[] original() {
        return original;
    }

    public long[] value() {
        return value;
    }

    @Override
    public void addToTooltip(net.minecraft.world.item.Item.TooltipContext context, java.util.function.Consumer<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag, net.minecraft.core.component.DataComponentGetter componentGetter) {
        if (original != null && original.length > 0 && original[0] != -1) {
            lines.accept(buildValueLine("Original", original));
        }
        lines.accept(buildValueLine("Value", value));
    }

    private static net.minecraft.network.chat.Component buildValueLine(String prefix, long[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(": ");
        boolean first = true;
        String[] names = {"Bronze", "Silver", "Gold"};
        int[] order = {2, 1, 0}; // Gold, Silver, Bronze
        for (int i : order) {
            long v = values[i];
            if (v <= 0) continue;
            if (!first) sb.append(", ");
            sb.append(v).append(' ').append(names[i]);
            first = false;
        }
        if (first) sb.append("0 Bronze");
        return net.minecraft.network.chat.Component.literal(sb.toString());
    }
}
