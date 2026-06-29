package com.glisco.numismaticoverhaul.client.gui;

import com.glisco.numismaticoverhaul.block.ShopOffer;
import com.glisco.numismaticoverhaul.block.ShopScreenHandler;
import com.glisco.numismaticoverhaul.currency.CurrencyResolver;
import com.glisco.numismaticoverhaul.network.UpdateShopScreenS2CPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.id;

public class ShopScreen extends AbstractContainerScreen<ShopScreenHandler> {

    private static final Identifier TEXTURE = id("textures/gui/shop_gui.png");
    private static final Identifier TRADES_TEXTURE = id("textures/gui/shop_gui_trades.png");

    private int selectedTab = 0;
    private final List<ShopOffer> offers = new ArrayList<>();
    private long storedCurrency = 0;
    private boolean transferEnabled = false;
    private int scrollOffset = 0;
    private ItemStack bufferStack = ItemStack.EMPTY;

    private int rpX;

    private Button storageTabBtn;
    private Button tradeTabBtn;
    private Button extractBtn;
    private Button transferToggleBtn;
    private EditBox priceField;
    private Button submitBtn;
    private Button deleteBtn;

    public ShopScreen(ShopScreenHandler handler, Inventory playerInventory, Component title) {
        super(handler, playerInventory, title, 416, 180);
        this.titleLabelY = 5;
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.rpX = this.leftPos + 180;

        // Tab buttons
        this.storageTabBtn = Button.builder(Component.empty(), b -> selectTab(0))
                .bounds(rpX + 2, topPos + 4, 50, 14).build();
        this.tradeTabBtn = Button.builder(Component.empty(), b -> selectTab(1))
                .bounds(rpX + 2, topPos + 22, 50, 14).build();
        this.addRenderableWidget(this.storageTabBtn);
        this.addRenderableWidget(this.tradeTabBtn);

        // Extract button (tab 0)
        this.extractBtn = Button.builder(Component.translatable("gui.numismatic-overhaul.shop.extract"), b -> this.menu.extractCurrency())
                .bounds(rpX + 2, topPos + 60, 90, 14).build();
        this.addRenderableWidget(this.extractBtn);

        // Transfer toggle (always visible)
        this.transferToggleBtn = Button.builder(Component.empty(), b -> this.menu.toggleTransfer())
                .bounds(rpX + 2, topPos + 80, 90, 14).build();
        this.transferToggleBtn.setTooltip(Tooltip.create(
                Component.translatable("gui.numismatic-overhaul.shop.transfer_tooltip.disabled")));
        this.addRenderableWidget(this.transferToggleBtn);

        // Price field — digit-only EditBox (tab 1)
        this.priceField = new EditBox(this.font, rpX + 2, topPos + 16, 90, 12, Component.empty()) {
            @Override
            public boolean charTyped(CharacterEvent event) {
                if (Character.isDigit(event.codepoint())) {
                    return super.charTyped(event);
                }
                return false;
            }
        };
        this.priceField.setMaxLength(7);
        this.priceField.setVisible(false);
        this.priceField.active = false;
        this.priceField.setResponder(s -> updateEditorButtons());
        this.addRenderableWidget(this.priceField);

        // Submit button (tab 1)
        this.submitBtn = Button.builder(Component.literal("Submit"), b -> {
            String val = this.priceField.getValue();
            if (!val.isBlank()) {
                try {
                    this.menu.createOffer(Long.parseLong(val));
                } catch (NumberFormatException ignored) {}
            }
        }).bounds(rpX + 2, topPos + 68, 44, 14).build();
        this.submitBtn.visible = false;
        this.submitBtn.active = false;
        this.addRenderableWidget(this.submitBtn);

        // Delete button (tab 1)
        this.deleteBtn = Button.builder(Component.literal("Delete"), b -> this.menu.deleteOffer())
                .bounds(rpX + 48, topPos + 68, 44, 14).build();
        this.deleteBtn.visible = false;
        this.deleteBtn.active = false;
        this.addRenderableWidget(this.deleteBtn);

        updateVisibility();
    }

    // ─── Tab Management ────────────────────────────────────────

    private void updateVisibility() {
        boolean storage = selectedTab == 0;
        this.storageTabBtn.active = !storage;
        this.tradeTabBtn.active = storage;

        this.extractBtn.visible = storage;
        this.extractBtn.active = storage;

        this.transferToggleBtn.visible = true;
        this.transferToggleBtn.active = true;

        this.priceField.setVisible(!storage);
        this.priceField.active = !storage;

        this.submitBtn.visible = !storage;
        this.submitBtn.active = false;
        this.deleteBtn.visible = !storage;
        this.deleteBtn.active = false;
    }

    private void selectTab(int index) {
        if (this.selectedTab == index) return;
        this.selectedTab = index;
        this.scrollOffset = 0;
        this.titleLabelY = (index == 0) ? 5 : 69420;
        updateVisibility();
        updateEditorButtons();
    }

    public int tab() {
        return this.selectedTab;
    }

    // ─── Rendering ─────────────────────────────────────────────

    @Override
    public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw custom background (do NOT call super.extractContents — it would stretch to 416x180)
        Identifier bg = selectedTab == 0 ? TEXTURE : TRADES_TEXTURE;
        context.blit(RenderPipelines.GUI_TEXTURED, bg, leftPos, topPos,
                0.0f, 0.0f, 176, 168, 256, 256);

        // Draw vanilla slots
        this.extractSlots(context, mouseX, mouseY);

        // Right panel background
        context.fill(rpX - 4, topPos, rpX + 100, topPos + imageHeight, 0xE0101010);

        // Tab labels in right panel
        context.text(this.font, "Storage", rpX + 16, topPos + 8,
                selectedTab == 0 ? 0xFFFF55 : 0xAAAAAA, false);
        context.text(this.font, "Trades", rpX + 18, topPos + 26,
                selectedTab == 1 ? 0xFFFF55 : 0xAAAAAA, false);

        if (selectedTab == 0) {
            renderStorageTab(context);
        } else {
            renderTradesTab(context, mouseX, mouseY);
        }

        renderTransferIndicator(context);
    }

    private void renderStorageTab(GuiGraphicsExtractor context) {
        long[] currency = CurrencyResolver.splitValues(this.storedCurrency);
        int y = topPos + 44;
        context.text(this.font, "Stored:", rpX + 2, y, 0xFFFFFF, false);
        context.text(this.font, "Gold: " + currency[2], rpX + 2, y + 12, 0xFFD700, false);
        context.text(this.font, "Silver: " + currency[1], rpX + 2, y + 24, 0xC0C0C0, false);
        context.text(this.font, "Bronze: " + currency[0], rpX + 2, y + 36, 0xCD7F32, false);
    }

    private void renderTradesTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // ── Offer list ──
        int listX = leftPos + 4;
        int listY = topPos + 4;
        int listW = 168;
        int listH = 60;
        context.fill(listX, listY, listX + listW, listY + listH, 0xE0101010);

        int maxVisible = 5;
        int end = Math.min(this.scrollOffset + maxVisible, this.offers.size());

        for (int i = this.scrollOffset; i < end; i++) {
            int row = i - this.scrollOffset;
            int itemY = listY + 2 + row * 12;

            boolean hovered = mouseX >= listX && mouseX < listX + listW
                    && mouseY >= itemY && mouseY < itemY + 12;
            if (hovered) {
                context.fill(listX, itemY, listX + listW, itemY + 12, 0x40FFFFFF);
            }

            ShopOffer offer = this.offers.get(i);
            context.item(offer.getSellStack(), listX + 2, itemY);
            context.text(this.font, String.valueOf(offer.getPrice()),
                    listX + 20, itemY + 2, 0x898989, false);
        }

        if (this.offers.size() > maxVisible) {
            context.text(this.font,
                    (this.scrollOffset + 1) + "/" + this.offers.size(),
                    listX + listW - 40, listY + listH + 2, 0xAAAAAA, false);
        }

        // ── Editor section ──
        int ey = topPos + 44;
        context.text(this.font, "Price:", rpX + 2, ey, 0xFFFFFF, false);

        // Buffer slot
        if (!this.bufferStack.isEmpty()) {
            int bx = rpX + 2;
            int by = ey + 14;
            context.fill(bx - 1, by - 1, bx + 17, by + 17, 0xFF373737);
            context.item(this.bufferStack, bx, by);
            context.itemDecorations(this.font, this.bufferStack, bx, by);
        }

        // Currency breakdown for current price
        String priceText = this.priceField.getValue();
        if (!priceText.isBlank()) {
            try {
                long price = Long.parseLong(priceText);
                long[] split = CurrencyResolver.splitValues(price);
                context.text(this.font,
                        split[2] + "G " + split[1] + "S " + split[0] + "B",
                        rpX + 20, ey + 18, 0x898989, false);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void renderTransferIndicator(GuiGraphicsExtractor context) {
        int y = topPos + 140;
        String label = this.transferEnabled ? "Transfer: ON" : "Transfer: OFF";
        int color = this.transferEnabled ? 0x28FFBF : 0xEB1D36;
        context.text(this.font, label, rpX + 2, y, color, false);
    }

    // ─── Post-render (tooltips) ────────────────────────────────

    @Override
    public void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        // Offer item tooltips
        if (selectedTab == 1) {
            int listX = leftPos + 4;
            int listY = topPos + 4;
            int maxVisible = 5;
            int end = Math.min(this.scrollOffset + maxVisible, this.offers.size());

            for (int i = this.scrollOffset; i < end; i++) {
                int row = i - this.scrollOffset;
                int itemY = listY + 2 + row * 12;
                if (mouseX >= listX + 2 && mouseX < listX + 18
                        && mouseY >= itemY && mouseY < itemY + 14) {
                    context.setTooltipForNextFrame(this.font,
                            this.offers.get(i).getSellStack(), mouseX, mouseY);
                }
            }

            // Buffer item tooltip
            if (!this.bufferStack.isEmpty()) {
                int bx = rpX + 2;
                int by = topPos + 58;
                if (mouseX >= bx && mouseX < bx + 16
                        && mouseY >= by && mouseY < by + 16) {
                    context.setTooltipForNextFrame(this.font,
                            this.bufferStack, mouseX, mouseY);
                }
            }
        }
    }

    // ─── Input Handling ────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (selectedTab == 1 && event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            // Offer list click
            int listX = leftPos + 4;
            int listY = topPos + 4;
            int listW = 168;
            int listH = 60;

            if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
                int maxVisible = 5;
                int row = (int) ((my - listY - 2) / 12);
                int offerIndex = this.scrollOffset + row;
                if (offerIndex >= 0 && offerIndex < this.offers.size()) {
                    ShopOffer offer = this.offers.get(offerIndex);
                    this.menu.loadOffer(offerIndex);
                    this.priceField.setValue(String.valueOf(offer.getPrice()));
                    return true;
                }
            }

            // Buffer slot click
            int bx = rpX + 2;
            int by = topPos + 58;
            if (mx >= bx && mx < bx + 16 && my >= by && my < by + 16) {
                this.menu.handleBufferClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedTab == 1) {
            int maxVisible = 5;
            int maxScroll = Math.max(0, this.offers.size() - maxVisible);
            this.scrollOffset = Mth.clamp(
                    this.scrollOffset - (int) verticalAmount, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ─── Data Update ───────────────────────────────────────────

    public void update(UpdateShopScreenS2CPacket data) {
        this.storedCurrency = data.storedCurrency();
        this.transferEnabled = data.transferEnabled();

        int prevOffers = this.offers.size();
        this.offers.clear();
        this.offers.addAll(data.offers());

        // Auto-scroll to bottom when new offers arrive (tab 1)
        if (selectedTab == 1 && this.offers.size() > prevOffers) {
            int maxVisible = 5;
            this.scrollOffset = Math.max(0, this.offers.size() - maxVisible);
        }

        this.bufferStack = data.tradeEditBuffer();
        this.menu.setTradeEditBuffer(data.tradeEditBuffer());

        // Update transfer tooltip
        this.transferToggleBtn.setTooltip(Tooltip.create(
                this.transferEnabled
                        ? Component.translatable("gui.numismatic-overhaul.shop.transfer_tooltip.enabled")
                        : Component.translatable("gui.numismatic-overhaul.shop.transfer_tooltip.disabled")));

        updateEditorButtons();
    }

    private void updateEditorButtons() {
        if (this.submitBtn == null || this.deleteBtn == null) return;

        String priceText = this.priceField.getValue();
        boolean hasPrice = !priceText.isBlank() && parsePrice(priceText) > 0;
        boolean hasBuffer = !this.bufferStack.isEmpty();
        boolean hasOffer = hasOfferFor(this.bufferStack);

        this.submitBtn.active = hasPrice && hasBuffer
                && (this.offers.size() < 24 || hasOffer);
        this.deleteBtn.active = hasOffer;
    }

    private long parsePrice(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean hasOfferFor(ItemStack stack) {
        return this.offers.stream()
                .anyMatch(offer -> ItemStack.isSameItem(stack, offer.getSellStack()));
    }
}
