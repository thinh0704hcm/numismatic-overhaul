package com.glisco.numismaticoverhaul.network;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.currency.CurrencyHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestPurseActionC2SPacket(Action action, long value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestPurseActionC2SPacket> ID =
        new CustomPacketPayload.Type<>(NumismaticOverhaul.id("request_purse_action"));

    public static final StreamCodec<FriendlyByteBuf, RequestPurseActionC2SPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, RequestPurseActionC2SPacket packet) {
            buf.writeEnum(packet.action);
            buf.writeLong(packet.value);
        }
        @Override
        public RequestPurseActionC2SPacket decode(FriendlyByteBuf buf) {
            Action action = buf.readEnum(Action.class);
            long value = buf.readLong();
            return new RequestPurseActionC2SPacket(action, value);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(RequestPurseActionC2SPacket packet, ServerPlayNetworking.Context ctx) {
        var player = ctx.player();
        var value = packet.value();

        switch (packet.action()) {
            case STORE_ALL ->
                ModComponents.get(player).modify(CurrencyHelper.getMoneyInInventory(player, true));
            case EXTRACT -> {
                var extracting = Math.max(0, Math.min(value, ModComponents.get(player).getValue()));
                CurrencyConverter.getAsItemStackList(extracting).forEach(stack -> player.getInventory().add(stack));
                ModComponents.get(player).modify(-extracting);
            }
            case EXTRACT_ALL -> {
                CurrencyConverter.getAsValidStacks(ModComponents.get(player).getValue())
                    .forEach(stack -> player.getInventory().add(stack));
                ModComponents.get(player).modify(-ModComponents.get(player).getValue());
            }
        }
    }

    public static RequestPurseActionC2SPacket storeAll() {
        return new RequestPurseActionC2SPacket(Action.STORE_ALL, 0);
    }

    public static RequestPurseActionC2SPacket extractAll() {
        return new RequestPurseActionC2SPacket(Action.EXTRACT_ALL, 0);
    }

    public static RequestPurseActionC2SPacket extract(long amount) {
        return new RequestPurseActionC2SPacket(Action.EXTRACT, amount);
    }

    public enum Action {
        STORE_ALL, EXTRACT, EXTRACT_ALL
    }
}
