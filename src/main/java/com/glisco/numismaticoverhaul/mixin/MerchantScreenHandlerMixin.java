package com.glisco.numismaticoverhaul.mixin;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.block.ShopMerchant;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.glisco.numismaticoverhaul.currency.CurrencyHelper;
import com.glisco.numismaticoverhaul.item.CoinItem;
import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public class MerchantScreenHandlerMixin {

    @Shadow
    @Final
    private Merchant trader;



    // Autofill with coins from the player's purse when trying to move items
    // Injected at TAIL of tryMoveItems(int)
    @Inject(method = "tryMoveItems", at = @At("TAIL"))
    public void autofillOverride(int slotIndex, CallbackInfo ci) {
        MerchantMenu handler = (MerchantMenu) (Object) this;
        Player player = this.trader.getTradingPlayer();
        if (player == null) return;

        CurrencyComponent playerBalance = ModComponents.get(player);
        MerchantOffers offers = handler.getOffers();
        if (slotIndex < 0 || slotIndex >= offers.size()) return;

        MerchantOffer offer = offers.get(slotIndex);
        // Check if the payment slot has coins from this mod
        ItemStack paymentStack = handler.getSlot(0).getItem();
        if (paymentStack.isEmpty()) return;

        if (paymentStack.getItem() instanceof CoinItem coinItem) {
            numismatic$autofillWithCoins(handler, playerBalance, coinItem, paymentStack.getCount());
        } else if (paymentStack.getItem() == NumismaticOverhaulItems.MONEY_BAG) {
            numismatic$autofillWithMoneyBag(handler, player, playerBalance, paymentStack);
        }

        playerBalance.commitTransactions();
    }

    @Unique
    private void numismatic$autofillWithCoins(MerchantMenu handler, CurrencyComponent playerBalance, CoinItem coinItem, int presentCount) {
        // Check the current offer's payment requirement
        MerchantOffer offer = handler.getOffers().get(0);
        long requiredCurrency = coinItem.currency.getRawValue(offer.getBaseCostA().getCount());
        long presentCurrency = coinItem.currency.getRawValue(presentCount);

        if (requiredCurrency <= presentCurrency) return;

        long neededCurrency = requiredCurrency - presentCurrency;
        if (neededCurrency > playerBalance.getValue()) return;

        playerBalance.pushTransaction(-neededCurrency);
    }

    @Unique
    private static void numismatic$autofillWithMoneyBag(MerchantMenu handler, Player player, CurrencyComponent playerBalance, ItemStack moneyBagStack) {
        long requiredCurrency = NumismaticOverhaulItems.MONEY_BAG.getValue(moneyBagStack);
        long availableCurrencyInPlayerInventory = CurrencyHelper.getMoneyInInventory(player, false);

        long neededCurrency = requiredCurrency - availableCurrencyInPlayerInventory;
        if (neededCurrency > playerBalance.getValue()) return;

        if (neededCurrency <= 0) {
            CurrencyHelper.deductFromInventory(player, requiredCurrency);
        } else {
            CurrencyHelper.deductFromInventory(player, availableCurrencyInPlayerInventory);
            playerBalance.pushTransaction(-neededCurrency);
        }
    }

    // Block villager "yes" sound for non-entity merchants (shop)
    @Inject(method = "playTradeSound", at = @At("HEAD"), cancellable = true)
    public void checkForEntityOnYes(CallbackInfo ci) {
        if (!(this.trader instanceof Entity)) ci.cancel();
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    public void thwartTaxEvasion(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.trader instanceof ShopMerchant shopMerchant)) return;

        var shop = shopMerchant.shop();
        if (shop.getLevel().getBlockEntity(shop.getBlockPos()) != shop || shop.getBlockPos().distSqr(player.blockPosition()) > 100) {
            cir.setReturnValue(false);
        }
    }

}
