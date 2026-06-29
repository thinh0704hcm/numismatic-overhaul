package com.glisco.numismaticoverhaul.block;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.client.gui.ShopScreen;
import com.glisco.numismaticoverhaul.network.ShopScreenHandlerRequestC2SPacket;
import com.glisco.numismaticoverhaul.network.UpdateShopScreenS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
public class ShopScreenHandler extends AbstractContainerMenu {
    private final Player owner;
    private final Container shopInventory;
    private ItemStack tradeEditBuffer = ItemStack.EMPTY;
    private final List<ShopOffer> offers;
    private ShopBlockEntity shop = null;
    public ShopScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(27));
    }
    public ShopScreenHandler(int syncId, Inventory playerInventory, Container shopInventory) {
        super(NumismaticOverhaul.SHOP_SCREEN_HANDLER_TYPE, syncId);
        this.shopInventory = shopInventory;
        this.owner = playerInventory.player;
        if (!this.owner.level().isClientSide()) {
            this.shop = (ShopBlockEntity) shopInventory;
            this.offers = shop.getOffers();
        } else {
            this.offers = new ArrayList<>();
        }
        // Shop inventory: 9x3 grid starting at (8, 17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new AutoHidingSlot(this.shopInventory, col + row * 9, 8 + col * 18, 17 + row * 18, 0, false));
            }
        }
        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }
    @Override
    public boolean stillValid(Player player) {
        return this.shopInventory.stillValid(player);
    }
    public void loadOffer(long index) {
        if (!this.owner.level().isClientSide()) {
            if (index > this.offers.size() - 1) {
                NumismaticOverhaul.LOGGER.error("Player {} attempted to load invalid trade at index {}", owner.getName(), index);
                return;
            }
            this.tradeEditBuffer = this.offers.get((int) index).getSellStack().copy();
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.LOAD_OFFER, index));
        }
    }
    public void createOffer(long price) {
        if (!this.owner.level().isClientSide()) {
            final var stack = this.tradeEditBuffer;
            if (stack.isEmpty()) return;
            this.shop.addOrReplaceOffer(new ShopOffer(stack, price));
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.CREATE_OFFER, price));
        }
    }
    public void extractCurrency() {
        if (!this.owner.level().isClientSide()) {
            ModComponents.get(owner).modify(shop.getStoredCurrency());
            this.shop.setStoredCurrency(0);
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.EXTRACT_CURRENCY));
        }
    }
    public void deleteOffer() {
        if (!this.owner.level().isClientSide()) {
            this.shop.deleteOffer(this.tradeEditBuffer);
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.DELETE_OFFER));
        }
    }
    public void toggleTransfer() {
        if (!this.owner.level().isClientSide()) {
            this.shop.toggleTransfer();
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.TOGGLE_TRANSFER));
        }
    }
    public void handleBufferClick() {
        if (!this.owner.level().isClientSide()) {
            this.tradeEditBuffer = this.getCarried().copy();
            this.updateClient();
        } else {
            ClientPlayNetworking.send(new ShopScreenHandlerRequestC2SPacket(ShopScreenHandlerRequestC2SPacket.Action.CLICK_BUFFER));
        }
    }
    private void updateClient() {
        ServerPlayNetworking.send((ServerPlayer) owner, new UpdateShopScreenS2CPacket(shop, this.tradeEditBuffer));
    }
    public ItemStack getBufferStack() {
        return this.tradeEditBuffer;
    }
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int containerSize = this.shopInventory.getContainerSize();
        if (index < containerSize) {
            if (!this.moveItemStackTo(stack, containerSize, containerSize + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, containerSize, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.shop != null) this.shop.busy = false;
    }
    private static class AutoHidingSlot extends Slot {
        private final int targetTab;
        private final boolean hide;
        public AutoHidingSlot(Container inventory, int index, int x, int y, int targetTab, boolean hide) {
            super(inventory, index, x, y);
            this.targetTab = targetTab;
            this.hide = hide;
        }
        @Override
        @Environment(EnvType.CLIENT)
        public boolean isActive() {
            if (!(Minecraft.getInstance().gui.screen() instanceof ShopScreen screen)) return true;
            //noinspection SimplifiableConditionalExpression
            return hide
                    ? screen.tab() != targetTab
                    : screen.tab() == targetTab;
        }
    }
    public void setTradeEditBuffer(ItemStack stack) {
        this.tradeEditBuffer = stack;
    }
}
