package com.glisco.numismaticoverhaul.currency;

import com.glisco.numismaticoverhaul.*;
import com.glisco.numismaticoverhaul.item.CoinItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.List;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.LOGGER;

public class CurrencyComponent {

    private final Player provider;
    private final List<Long> transactions;

    public CurrencyComponent(Player provider) {
        this.provider = provider;
        this.transactions = new ArrayList<>();
    }

    public long getValue() {
        long[] data = ModComponents.getCurrencyData(provider.getUUID());
        return data[0] + data[1] * 100 + data[2] * 10000;
    }

    @Deprecated
    public void setValue(long value) {
        var split = CurrencyResolver.splitValues(value);
        ModComponents.setCurrencyData(provider.getUUID(), split);
    }

    public void modify(long value) {
        setValue(getValue() + value);

        long tempValue = value < 0 ? -value : value;
        List<ItemStack> transactionStacks = CurrencyConverter.getAsItemStackList(tempValue);
        if (transactionStacks.isEmpty()) return;

        if (provider.level().isClientSide()) return;

        var moneyMessageLocation = NumismaticOverhaul.CONFIG.moneyMessageLocation();
        if (moneyMessageLocation == MoneyMessageLocation.DISABLED) return;

        var message = moneyMessageLocation == MoneyMessageLocation.CHAT
            ?
            net.minecraft.network.chat.Component.literal("numismatic §> ").withStyle(s -> s.withColor(Currency.GOLD.getNameColor()))
            :
            net.minecraft.network.chat.Component.empty();

        message.append(value < 0 ? net.minecraft.network.chat.Component.literal("§c- ") : net.minecraft.network.chat.Component.literal("§a+ "));
        message.append(net.minecraft.network.chat.Component.literal("§7["));
        for (ItemStack stack : transactionStacks) {
            message.append(net.minecraft.network.chat.Component.literal("§b" + stack.getCount() + " "));
            message.append(net.minecraft.network.chat.Component.translatable(
                "currency.numismatic-overhaul." + ((CoinItem) stack.getItem()).currency.name().toLowerCase()
            ).withStyle(s -> s.withColor(((CoinItem) stack.getItem()).currency.getNameColor())));

            if (transactionStacks.indexOf(stack) != transactionStacks.size() - 1) {
                message.append(net.minecraft.network.chat.Component.literal(", "));
            }
        }
        message.append(net.minecraft.network.chat.Component.literal("§7]"));

        if (moneyMessageLocation == MoneyMessageLocation.ACTIONBAR) {
            if (provider instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(message);
            } else {
                provider.sendSystemMessage(message);
            }
        } else {
            provider.sendSystemMessage(message);
        }
    }

    public void silentModify(long value) {
        setValue(getValue() + value);
    }

    public void pushTransaction(long value) {
        this.transactions.add(value);
    }

    public Long popTransaction() {
        return this.transactions.removeLast();
    }

    public void commitTransactions() {
        this.modify(this.transactions.stream().mapToLong(Long::longValue).sum());
        this.transactions.clear();
    }
}
