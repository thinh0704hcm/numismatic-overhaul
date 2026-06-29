package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.Slot;
import java.util.function.Predicate;


public class PiggyBankScreenHandler extends AbstractContainerMenu {

    private final ContainerLevelAccess context;

    public PiggyBankScreenHandler(int index, Inventory playerInventory) {
        this(index, playerInventory, ContainerLevelAccess.create(null, BlockPos.ZERO), new SimpleContainer(3));
    }

    public PiggyBankScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context, Container piggyBankInventory) {
        super(NumismaticOverhaul.PIGGY_BANK_SCREEN_HANDLER_TYPE, syncId);
        this.context = context;

        this.addSlot(new ValidatingSlot(piggyBankInventory, 0, 62, 26, stack -> stack.getItem() == NumismaticOverhaulItems.BRONZE_COIN));
        this.addSlot(new ValidatingSlot(piggyBankInventory, 1, 80, 26, stack -> stack.getItem() == NumismaticOverhaulItems.SILVER_COIN));
        this.addSlot(new ValidatingSlot(piggyBankInventory, 2, 98, 26, stack -> stack.getItem() == NumismaticOverhaulItems.GOLD_COIN));

                // Player inventory at (8, 63)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 63 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 121));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 3) {
            if (!this.moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, 3, false)) {
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
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(context, player, NumismaticOverhaulBlocks.PIGGY_BANK);
    }
    private static class ValidatingSlot extends Slot {
        private final Predicate<ItemStack> validator;

        public ValidatingSlot(Container inventory, int index, int x, int y, Predicate<ItemStack> validator) {
            super(inventory, index, x, y);
            this.validator = validator;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return validator.test(stack);
        }
    }
}
