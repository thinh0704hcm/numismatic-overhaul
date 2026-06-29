package com.glisco.numismaticoverhaul.client.gui;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.Currency;
import com.glisco.numismaticoverhaul.currency.CurrencyResolver;
import com.glisco.numismaticoverhaul.network.RequestPurseActionC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Vanilla replacement for owo-based PurseLayerElement.
 * Manages the purse overlay popup on inventory/creative/merchant screens.
 */
public class PurseOverlay {

    private static final Identifier PURSE_TEXTURE = NumismaticOverhaul.id("textures/gui/purse_widget.png");

    // Popup state
    public static boolean popupOpen = false;

    // Selected amounts for extraction
    public static int selectedBronze = 0;
    public static int selectedSilver = 0;
    public static int selectedGold = 0;

    // Popup button hit-test bounds [x1, y1, x2, y2]
    private static int[] goldIncBounds, goldDecBounds;
    private static int[] silverIncBounds, silverDecBounds;
    private static int[] bronzeIncBounds, bronzeDecBounds;
    private static int[] extractBounds;
    private static int[] popupBounds;

    /**
     * Handle mouse click events. Return true to allow the click, false to cancel it.
     * Called from ScreenMouseEvents.AllowMouseClick.
     */
    public static boolean handleMouseClick(Screen screen, MouseButtonEvent event) {
        if (!popupOpen) return true;

        double mx = event.x();
        double my = event.y();

        // Click inside popup: consume and handle
        if (inBounds(mx, my, popupBounds)) {
            handleClick(event);
            return false;
        }

        // Click outside popup: close it, let click through
        popupOpen = false;
        return true;
    }

    /**
     * Render the purse overlay. Called from ScreenEvents.afterExtract callback.
     */
    public static void render(Screen screen, GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        int screenX = containerScreen.leftPos;
        int screenY = containerScreen.topPos;

        int buttonX, buttonY;
        if (screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) {
            buttonX = screenX + 38 + NumismaticOverhaul.CONFIG.purseOffsets().creativeX();
            buttonY = screenY + 4 + NumismaticOverhaul.CONFIG.purseOffsets().creativeY();
        } else if (screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen) {
            buttonX = screenX + 260 + NumismaticOverhaul.CONFIG.purseOffsets().merchantX();
            buttonY = screenY + 5 + NumismaticOverhaul.CONFIG.purseOffsets().merchantY();
        } else {
            // Survival inventory
            buttonX = screenX + 160 + NumismaticOverhaul.CONFIG.purseOffsets().survivalX();
            buttonY = screenY + 5 + NumismaticOverhaul.CONFIG.purseOffsets().survivalY();
        }

        // Draw purse button (11x13, UV 62,0)
        context.blit(RenderPipelines.GUI_TEXTURED, PURSE_TEXTURE,
                buttonX, buttonY, 62.0f, 0.0f, 11, 13, 128, 64);

        // Render popup if open
        if (popupOpen) {
            renderPopup(context, buttonX, buttonY);
        }
    }

    /**
     * Compute popup position (37x59, offset left-30 top+15 from button).
     */
    private static int popupX(int buttonX) {
        return buttonX - 30;
    }

    private static int popupY(int buttonY) {
        return buttonY + 15;
    }

    private static void renderPopup(GuiGraphicsExtractor context, int buttonX, int buttonY) {
        int px = popupX(buttonX);
        int py = popupY(buttonY);

        // Popup background (37x59, UV 0,0)
        context.blit(RenderPipelines.GUI_TEXTURED, PURSE_TEXTURE,
                px, py, 0.0f, 0.0f, 37, 59, 128, 64);

        var font = Minecraft.getInstance().font;

        // Draw currency counts at (px+5, py+12) with gap=11
        int textY = py + 12;

        context.text(font, Component.literal(String.valueOf(selectedGold)), px + 5, textY, Currency.GOLD.getNameColor());
        textY += 11;
        context.text(font, Component.literal(String.valueOf(selectedSilver)), px + 5, textY, Currency.SILVER.getNameColor());
        textY += 11;
        context.text(font, Component.literal(String.valueOf(selectedBronze)), px + 5, textY, Currency.BRONZE.getNameColor());

        // Increment buttons at (px+18, py+10) with gap=6, each 9x5
        int btnY = py + 10;
        goldIncBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, true);
        btnY += 6;
        goldDecBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, false);

        btnY += 6;
        silverIncBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, true);
        btnY += 6;
        silverDecBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, false);

        btnY += 6;
        bronzeIncBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, true);
        btnY += 6;
        bronzeDecBounds = new int[]{px + 18, btnY, px + 18 + 9, btnY + 5};
        drawAdjustButton(context, px + 18, btnY, false);

        // Extract button at (px+3, py+46), 24x8, UV 37,0
        extractBounds = new int[]{px + 3, py + 46, px + 3 + 24, py + 46 + 8};
        context.blit(RenderPipelines.GUI_TEXTURED, PURSE_TEXTURE,
                px + 3, py + 46, 37.0f, 0.0f, 24, 8, 128, 64);

        // Update popup bounds
        popupBounds = new int[]{px, py, px + 37, py + 59};
    }

    private static void drawAdjustButton(GuiGraphicsExtractor context, int x, int y, boolean increment) {
        // Increment: u=37,v=24  Decrement: u=46,v=24  (both 9x5)
        float u = increment ? 37.0f : 46.0f;
        context.blit(RenderPipelines.GUI_TEXTURED, PURSE_TEXTURE,
                x, y, u, 24.0f, 9, 5, 128, 64);
    }

    private static void handleClick(MouseButtonEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var comp = ModComponents.get(mc.player);
        long totalValue = comp.getValue();
        long[] split = CurrencyResolver.splitValues(totalValue);
        long bronze = split[0], silver = split[1], gold = split[2];

        double mx = event.x(), my = event.y();

        // Gold increment/decrement
        if (inBounds(mx, my, goldIncBounds)) {
            adjust(selectedGold, gold, Currency.GOLD, mc.options.keyShift.isDown() ? 10 : 1, v -> selectedGold = v);
        } else if (inBounds(mx, my, goldDecBounds)) {
            adjust(selectedGold, gold, Currency.GOLD, mc.options.keyShift.isDown() ? -10 : -1, v -> selectedGold = v);
        }
        // Silver increment/decrement
        else if (inBounds(mx, my, silverIncBounds)) {
            adjust(selectedSilver, silver, Currency.SILVER, mc.options.keyShift.isDown() ? 10 : 1, v -> selectedSilver = v);
        } else if (inBounds(mx, my, silverDecBounds)) {
            adjust(selectedSilver, silver, Currency.SILVER, mc.options.keyShift.isDown() ? -10 : -1, v -> selectedSilver = v);
        }
        // Bronze increment/decrement
        else if (inBounds(mx, my, bronzeIncBounds)) {
            adjust(selectedBronze, bronze, Currency.BRONZE, mc.options.keyShift.isDown() ? 10 : 1, v -> selectedBronze = v);
        } else if (inBounds(mx, my, bronzeDecBounds)) {
            adjust(selectedBronze, bronze, Currency.BRONZE, mc.options.keyShift.isDown() ? -10 : -1, v -> selectedBronze = v);
        }
        // Extract button
        else if (inBounds(mx, my, extractBounds)) {
            long value = CurrencyResolver.combineValues(new long[]{selectedBronze, selectedSilver, selectedGold});

            if (mc.options.keyShift.isDown() && mc.options.keySprint.isDown()) {
                ClientPlayNetworking.send(RequestPurseActionC2SPacket.extractAll());
            } else if (value > 0) {
                ClientPlayNetworking.send(RequestPurseActionC2SPacket.extract(value));
                comp.silentModify(-value);
                selectedBronze = 0;
                selectedSilver = 0;
                selectedGold = 0;
            }
        }
    }

    private static void adjust(int current, long available, Currency currency, int adjustBy, java.util.function.IntConsumer setter) {
        long stepSize = currency.getRawValue(1);
        long totalSelectedRaw = CurrencyResolver.combineValues(new long[]{selectedBronze, selectedSilver, selectedGold});
        long totalAvailable = ModComponents.get(Minecraft.getInstance().player).getValue();
        long remainingForThisCurrency = (totalAvailable - totalSelectedRaw + current * stepSize) / stepSize;

        long by = Math.min(adjustBy, remainingForThisCurrency);
        int newVal = Mth.clamp(current + (int) by, 0, 99);
        setter.accept(newVal);
    }

    private static boolean inBounds(double x, double y, int[] bounds) {
        return bounds != null && x >= bounds[0] && x < bounds[2] && y >= bounds[1] && y < bounds[3];
    }
}
