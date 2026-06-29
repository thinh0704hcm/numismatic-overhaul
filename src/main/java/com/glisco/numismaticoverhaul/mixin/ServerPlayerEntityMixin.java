package com.glisco.numismaticoverhaul.mixin;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "destroyVanishingCursedItems", at = @At("TAIL"))
    public void onServerDeath(CallbackInfo ci) {
        var player = (Player) (Object) this;

        final var world = player.level();
        if (world.isClientSide()) return;

        final var component = ModComponents.get(player);

        var dropPercentage = ((ServerLevel)world).getGameRules().get(NumismaticOverhaul.MONEY_DROP_PERCENTAGE) * .01f;
        int dropped = (int) (component.getValue() * dropPercentage);

        var stacksDropped = CurrencyConverter.getAsValidStacks(dropped);
        for (var drop : stacksDropped) {
            for (int i = 0; i < drop.getCount(); i++) {
                player.drop(drop.copy().split(1), true);
            }
        }

        component.modify(-dropped);
    }

}
