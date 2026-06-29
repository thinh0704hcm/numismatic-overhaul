package com.glisco.numismaticoverhaul.network;

import com.glisco.numismaticoverhaul.block.ShopScreenHandler;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShopScreenHandlerRequestC2SPacket(Action action, long value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShopScreenHandlerRequestC2SPacket> ID = new CustomPacketPayload.Type<>(NumismaticOverhaul.id("shop_screen_handler_request"));
    public static final StreamCodec<FriendlyByteBuf, ShopScreenHandlerRequestC2SPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, ShopScreenHandlerRequestC2SPacket packet) {
            packet.write(buf);
        }
        @Override
        public ShopScreenHandlerRequestC2SPacket decode(FriendlyByteBuf buf) {
            return ShopScreenHandlerRequestC2SPacket.read(buf);
        }
    };

    public ShopScreenHandlerRequestC2SPacket(Action action) {
        this(action, 0);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeLong(value);
    }

    public static ShopScreenHandlerRequestC2SPacket read(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        long value = buf.readLong();
        return new ShopScreenHandlerRequestC2SPacket(action, value);
    }

    public static void handle(ShopScreenHandlerRequestC2SPacket packet, ServerPlayNetworking.Context ctx) {
        var player = ctx.player();
        var value = packet.value();
        if (!(player.containerMenu instanceof ShopScreenHandler shopHandler)) return;
        switch (packet.action()) {
            case LOAD_OFFER -> shopHandler.loadOffer(value);
            case CREATE_OFFER -> shopHandler.createOffer(value);
            case DELETE_OFFER -> shopHandler.deleteOffer();
            case EXTRACT_CURRENCY -> shopHandler.extractCurrency();
            case TOGGLE_TRANSFER -> shopHandler.toggleTransfer();
            case CLICK_BUFFER -> shopHandler.handleBufferClick();
        }
    }



    public enum Action {
        CREATE_OFFER, DELETE_OFFER, LOAD_OFFER, EXTRACT_CURRENCY, TOGGLE_TRANSFER, CLICK_BUFFER
    }
}
