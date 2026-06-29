package com.glisco.numismaticoverhaul.client.gui;

import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.item.CurrencyTooltipProvider;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CurrencyTooltipComponent implements ClientTooltipComponent {

    private final CurrencyTooltipProvider data;
    private final List<Component> text;

    private int widthCache = -1;

    public CurrencyTooltipComponent(CurrencyTooltipProvider data) {
        this.data = data;
        this.text = new ArrayList<>();

        if (data.original()[0] != -1) {
            CurrencyConverter.getAsItemStackList(data.original()).forEach(stack -> text.add(Component.literal(String.valueOf(stack.getCount())).withStyle(ChatFormatting.GRAY)));
        }

        CurrencyConverter.getAsItemStackList(data.value()).forEach(stack -> text.add(Component.literal(String.valueOf(stack.getCount())).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public int getHeight(Font textRenderer) {
        return 10 * text.size();
    }

    @Override
    public int getWidth(Font textRenderer) {
        if (widthCache == -1) {
            widthCache = textRenderer.width(text.stream()
                    .max(Comparator.comparingInt(textRenderer::width)).orElse(Component.empty())) + 10;
        }
        return widthCache;
    }

@Override
    public void extractText(net.minecraft.client.gui.GuiGraphicsExtractor context, Font textRenderer, int x, int y) {
        for (int i = 0; i < text.size(); i++) {
            context.text(textRenderer, text.get(i), x, y + i * 10, -1, false);
        }
    }

    

}
