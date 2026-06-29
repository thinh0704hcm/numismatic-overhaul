package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.*;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.WorldlyContainer;
import java.util.*;
import java.util.stream.IntStream;

public class ShopBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, MenuProvider {

    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private static final int[] NO_SLOTS = new int[0];


    private NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);

    public boolean busy = false;
    private final Merchant merchant;
    private List<ShopOffer> offers;

    private long storedCurrency;
    private UUID owner;
    private boolean allowsTransfer = false;

    private int tradeIndex;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(NumismaticOverhaulBlocks.Entities.SHOP, pos, state);

        boolean inexhaustible = (state.getBlock() instanceof ShopBlock shop) && shop.inexhaustible();
        this.merchant = new ShopMerchant(this, inexhaustible);

        this.offers = new ArrayList<>();
        this.storedCurrency = 0;
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.inventory = items;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return allowsTransfer ? SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return allowsTransfer;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("gui.numismatic-overhaul.shop.inventory_title");
    }

    @NotNull
    public ShopMerchant getMerchant() {
        return (ShopMerchant) merchant;
    }

    public List<ShopOffer> getOffers() {
        return offers;
    }

    public long getStoredCurrency() {
        return storedCurrency;
    }

    public boolean isTransferEnabled() {
        return allowsTransfer;
    }

    public void toggleTransfer() {
        this.allowsTransfer = !this.allowsTransfer;
    }

    public void setStoredCurrency(long storedCurrency) {
        this.storedCurrency = storedCurrency;
        setChanged();
    }

    public void addCurrency(long value) {
        this.storedCurrency += value;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.inventory);
        tag.store("Offers", ShopOffer.CODEC.listOf(), this.offers);
        tag.putBoolean("AllowsTransfer", this.allowsTransfer);
        tag.putLong("StoredCurrency", storedCurrency);
        if (owner != null) {
            tag.putLong("OwnerMost", owner.getMostSignificantBits());
            tag.putLong("OwnerLeast", owner.getLeastSignificantBits());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, this.inventory);
        this.offers = tag.<List<ShopOffer>>read("Offers", ShopOffer.CODEC.listOf()).orElse(new ArrayList<>());
        long most = tag.getLongOr("OwnerMost", 0L);
        long least = tag.getLongOr("OwnerLeast", 0L);
        owner = (most != 0L || least != 0L) ? new UUID(most, least) : null;
        this.allowsTransfer = tag.getBooleanOr("AllowsTransfer", false);
        this.storedCurrency = tag.getLongOr("StoredCurrency", 0L);
    }

    public void addOrReplaceOffer(ShopOffer offer) {
        int indexToReplace = -1;
        for (int i = 0; i < offers.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(offer.getSellStack(), offers.get(i).getSellStack())) continue;
            indexToReplace = i;
            break;
        }

        if (indexToReplace == -1) {
            if (offers.size() >= 24) {
                NumismaticOverhaul.LOGGER.error("Tried adding more than 24 trades to shop at {}", this.worldPosition);
                return;
            }
            offers.add(offer);
        } else {
            offers.set(indexToReplace, offer);
        }
        this.setChanged();
    }

    public void deleteOffer(ItemStack stack) {
        if (!offers.removeIf(offer -> ItemStack.isSameItemSameComponents(stack, offer.getSellStack()))) {
            NumismaticOverhaul.LOGGER.error("Tried to delete invalid trade for {} from shop at {}", stack, this.worldPosition);
            return;
        }
        this.setChanged();
    }

    public static void tick(Level level, BlockPos ignoredPos, BlockState ignoredState, ShopBlockEntity blockEntity) {
        blockEntity.tick(level);
    }

    public void tick(Level level) {
        if (level.getLevelData().getGameTime() % 60 == 0) tradeIndex++;
    }

    @Environment(EnvType.CLIENT)
    public ItemStack getItemToRender() {
        if (tradeIndex > offers.size() - 1) tradeIndex = 0;
        return offers.get(tradeIndex).getSellStack();
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new ShopScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null) return false;
        return player.getUUID().equals(this.owner) && this.level.getBlockEntity(this.worldPosition) == this && this.worldPosition.distSqr(player.blockPosition()) <= 100;
    }



    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // getUpdateTag handled by parent BaseContainerBlockEntity
}
