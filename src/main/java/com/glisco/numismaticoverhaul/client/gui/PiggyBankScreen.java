package com.glisco.numismaticoverhaul.client.gui;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.block.PiggyBankScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PiggyBankScreen extends AbstractContainerScreen<PiggyBankScreenHandler> {

    private static final Identifier TEXTURE = NumismaticOverhaul.id("textures/gui/piggy_bank.png");

    public PiggyBankScreen(PiggyBankScreenHandler handler, Inventory playerInventory, Component title) {
        super(handler, playerInventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = (this.imageWidth - Minecraft.getInstance().font.width(title)) / 2;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractContents(context, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        // Draw full background (176x166 region from the 256x256 texture)
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw coin slot hints when slots are empty
        // Bronze hint: texture region (0, 145) 16x16
        if (!this.menu.getSlot(0).hasItem()) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 62, y + 26, 0.0f, 145.0f, 16, 16, 256, 256);
        }
        // Silver hint: texture region (16, 145) 16x16
        if (!this.menu.getSlot(1).hasItem()) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 80, y + 26, 16.0f, 145.0f, 16, 16, 256, 256);
        }
        // Gold hint: texture region (32, 145) 16x16
        if (!this.menu.getSlot(2).hasItem()) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 98, y + 26, 32.0f, 145.0f, 16, 16, 256, 256);
        }
    }
}
