package com.glisco.numismaticoverhaul.mixin;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.glisco.numismaticoverhaul.NumismaticOverhaul.CONFIG;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow @Nullable protected net.minecraft.world.entity.EntityReference<Player> lastHurtByPlayer;

    @Shadow public abstract float getMaxHealth();

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    public void injectCoins(ServerLevel level, DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (this.lastHurtByPlayer == null || this.lastHurtByPlayer.getEntity(this.level(), Player.class) == null) return;
        // Handle config dependent coin drops
        var entityType = this.getType();
        if (NumismaticOverhaul.MOBS_IN_BOURGEOISIE.containsKey(entityType)) {
            long baseValue = NumismaticOverhaul.MOBS_IN_BOURGEOISIE.get(entityType);
            float variance = ((ServerLevel)this.level()).getGameRules().get(NumismaticOverhaul.MONEY_MOB_DROP_VARIANCE) * .01f;
            if (variance > 0.02f) {
                variance = Mth.randomBetween(random, 1.0f - variance, 1.0f + variance);
            } else {
                variance = 1.0f;
            }
            if (CONFIG.scaleOnHealth()) {
                variance *= (this.getMaxHealth() / (20 * CONFIG.healthScaleReduction()));
            }
            long finalValue = Mth.clamp(((long) (baseValue * variance)), 0, Long.MAX_VALUE);
            var moneyStacks = CurrencyHelper.getAsStacks(finalValue, 4);
            moneyStacks.forEach(stack -> this.spawnAtLocation((ServerLevel)this.level(), stack));
        }
    }

}
