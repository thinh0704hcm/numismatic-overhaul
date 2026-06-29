package com.glisco.numismaticoverhaul.client;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.block.NumismaticOverhaulBlocks;
import com.glisco.numismaticoverhaul.client.gui.PiggyBankScreen;
import com.glisco.numismaticoverhaul.client.gui.PurseOverlay;
import com.glisco.numismaticoverhaul.client.gui.ShopScreen;
import com.glisco.numismaticoverhaul.mixin.ScreenAccessor;
import com.glisco.numismaticoverhaul.network.RequestPurseActionC2SPacket;
import com.glisco.numismaticoverhaul.network.UpdateShopScreenS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.glisco.numismaticoverhaul.client.ShopBlockEntityRender;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class NumismaticOverhaulClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(NumismaticOverhaul.SHOP_SCREEN_HANDLER_TYPE, ShopScreen::new);
        MenuScreens.register(NumismaticOverhaul.PIGGY_BANK_SCREEN_HANDLER_TYPE, PiggyBankScreen::new);

        BlockEntityRenderers.register(NumismaticOverhaulBlocks.Entities.SHOP, ShopBlockEntityRender::new);

        // Register purse overlay on target screens
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen || screen instanceof MerchantScreen)) {
                return;
            }

            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;

            // Compute button position based on screen type
            int buttonX, buttonY;
            if (screen instanceof CreativeModeInventoryScreen) {
                buttonX = containerScreen.leftPos + 38 + NumismaticOverhaul.CONFIG.purseOffsets().creativeX();
                buttonY = containerScreen.topPos + 4 + NumismaticOverhaul.CONFIG.purseOffsets().creativeY();
            } else if (screen instanceof MerchantScreen) {
                buttonX = containerScreen.leftPos + 260 + NumismaticOverhaul.CONFIG.purseOffsets().merchantX();
                buttonY = containerScreen.topPos + 5 + NumismaticOverhaul.CONFIG.purseOffsets().merchantY();
            } else {
                buttonX = containerScreen.leftPos + 160 + NumismaticOverhaul.CONFIG.purseOffsets().survivalX();
                buttonY = containerScreen.topPos + 5 + NumismaticOverhaul.CONFIG.purseOffsets().survivalY();
            }

            // Invisible clickable button for purse toggle (purse icon is rendered by PurseOverlay)
            AbstractButton purseButton = new AbstractButton(buttonX, buttonY, 11, 13, Component.empty()) {
                @Override
                public void onPress(net.minecraft.client.input.InputWithModifiers input) {
                    if (net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown()) {
                        ClientPlayNetworking.send(RequestPurseActionC2SPacket.storeAll());
                    } else {
                        PurseOverlay.popupOpen = !PurseOverlay.popupOpen;
                    }
                }

                @Override
                protected void extractContents(
                        net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
                    // Invisible — purse icon is drawn by PurseOverlay.render()
                }

                @Override
                protected void updateWidgetNarration(
                        net.minecraft.client.gui.narration.NarrationElementOutput builder) {
                    defaultButtonNarrationText(builder);
                }
            };

            ((ScreenAccessor) screen).numismatic$addRenderableWidget(purseButton);

            // Render purse overlay after screen content
            ScreenEvents.afterExtract(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                PurseOverlay.render(s, drawContext, mouseX, mouseY, tickDelta);
            });

            // Intercept mouse clicks for popup handling
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                return PurseOverlay.handleMouseClick(s, event);
            });
        });

        // Register S2C packet handler
        ClientPlayNetworking.registerGlobalReceiver(UpdateShopScreenS2CPacket.ID, UpdateShopScreenS2CPacket::handle);
    }
}
