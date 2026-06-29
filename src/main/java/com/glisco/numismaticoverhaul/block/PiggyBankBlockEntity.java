package com.glisco.numismaticoverhaul.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PiggyBankBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    public PiggyBankBlockEntity(BlockPos pos, BlockState state) {
        super(NumismaticOverhaulBlocks.Entities.PIGGY_BANK, pos, state);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.inventory);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.numismatic-overhaul.piggy_bank");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    public NonNullList<ItemStack> inventory() {
        return this.inventory;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv) {
        return new PiggyBankScreenHandler(syncId, inv, ContainerLevelAccess.create(this.level, this.worldPosition), this);
    }
}
