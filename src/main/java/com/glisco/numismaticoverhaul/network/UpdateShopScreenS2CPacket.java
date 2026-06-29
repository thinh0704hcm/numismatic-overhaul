package com.glisco.numismaticoverhaul.network;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.block.ShopBlockEntity;
import com.glisco.numismaticoverhaul.block.ShopOffer;
import com.glisco.numismaticoverhaul.client.gui.ShopScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public record UpdateShopScreenS2CPacket(List<ShopOffer> offers, long storedCurrency, boolean transferEnabled, ItemStack tradeEditBuffer) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateShopScreenS2CPacket> ID = new CustomPacketPayload.Type<>(NumismaticOverhaul.id("update_shop_screen"));
    public static final StreamCodec<FriendlyByteBuf, UpdateShopScreenS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, UpdateShopScreenS2CPacket packet) {
            packet.write(buf);
        }
        @Override
        public UpdateShopScreenS2CPacket decode(FriendlyByteBuf buf) {
            return UpdateShopScreenS2CPacket.read(buf);
        }
    };

    public UpdateShopScreenS2CPacket(ShopBlockEntity shop, ItemStack tradeEditBuffer) {
        this(shop.getOffers(), shop.getStoredCurrency(), shop.isTransferEnabled(), tradeEditBuffer);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(offers, (b, offer) -> ShopOffer.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) b, offer));
        buf.writeLong(storedCurrency);
        buf.writeBoolean(transferEnabled);
        buf.writeWithCodec(NbtOps.INSTANCE, ItemStack.OPTIONAL_CODEC, tradeEditBuffer);
    }

    public static UpdateShopScreenS2CPacket read(FriendlyByteBuf buf) {
        List<ShopOffer> offers = buf.readCollection(java.util.ArrayList::new, s -> ShopOffer.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) s));
        long storedCurrency = buf.readLong();
        boolean transferEnabled = buf.readBoolean();
        ItemStack tradeEditBuffer = buf.readWithCodec(NbtOps.INSTANCE, ItemStack.OPTIONAL_CODEC, NbtAccounter.unlimitedHeap());
        return new UpdateShopScreenS2CPacket(offers, storedCurrency, transferEnabled, tradeEditBuffer);
    }

    public static void handle(UpdateShopScreenS2CPacket packet, ClientPlayNetworking.Context ctx) {
        ctx.client().execute(() -> {
            if (ctx.client().gui.screen() instanceof ShopScreen screen) {
                screen.update(packet);
            }
        });
    }
}
