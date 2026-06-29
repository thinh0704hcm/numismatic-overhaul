package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.currency.CurrencyHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ShopMerchant implements Merchant {

    private final ShopBlockEntity shop;
    private final boolean inexhaustible;
    private MerchantOffers recipeList = new MerchantOffers();
    private Player tradingPlayer;

    public ShopMerchant(ShopBlockEntity blockEntity, boolean inexhaustible) {
        this.shop = blockEntity;
        this.inexhaustible = inexhaustible;
    }

    public void updateTrades() {
        recipeList.clear();
        shop.getOffers().forEach(offer -> recipeList.add(offer.toTradeOffer(shop, this.inexhaustible)));
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
        this.shop.busy = player != null;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return recipeList;
    }

    @Override
    public void overrideOffers(@Nullable MerchantOffers offers) {
        this.recipeList = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        // Consume items
        ItemStack costA = offer.getBaseCostA();
        ItemStack costB = offer.getCostB();
        offer.take(costA, costB);
        if (!this.inexhaustible) {
            ShopOffer.remove(shop.getItems(), offer.getResult());
            this.updateTrades();
            if (this.getTradingPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundMerchantOffersPacket(
                        serverPlayer.containerMenu.containerId,
                        this.recipeList,
                        0, 0, false, false
                ));
            }
        }
        var items = new ArrayList<ItemStack>();
        items.add(costA);
        if (!costB.isEmpty()) {
            items.add(costB);
        }
        shop.addCurrency(CurrencyHelper.getValue(items));
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        // No additional behavior needed
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int experience) {
        // No behavior needed
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public ShopBlockEntity shop() {
        return this.shop;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }
}
