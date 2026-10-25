package org.onenonly.bitsandbalance.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.network.SecondChanceToastPayload;
/**
 * Second Chance: prevent unfair one-shots above the configured health threshold.
 * Leaves player at half a heart and applies Resistance I and Nausea II for 10s.
 * Excludes fall damage if configured.
 * Includes configurable cooldown in Minecraft days, stored per-player.
 */
@Mixin(LivingEntity.class)
public abstract class SecondChanceMixin {

    private static final String NBT_KEY = "rebalance_second_chance_next_allowed";

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void rebalance$secondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        rebalance$trySecondChance(source, amount, cir);
    }

    // In 1.21.x, server-side damage commonly routes through hurtServer.
    // Keep this injection optional so the mod still loads on mappings/versions where it doesn't exist.
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true, require = 0)
    private void rebalance$secondChanceServer(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        rebalance$trySecondChance(source, amount, cir);
    }

    private void rebalance$trySecondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableSecondChance) return;

        LivingEntity self = (LivingEntity) (Object) this;
        // Server-side only; client should not make gameplay decisions
        if (self.level().isClientSide()) return;
        if (!(self instanceof Player player)) return;

        // Exclude fall damage when configured
        if (Config.secondChanceExcludeFall) {
            try {
                if (source.is(DamageTypes.FALL)) {
                    return;
                }
            } catch (Throwable ignored) {
                // If mappings differ, fail open (do not trigger on unknown fall check)
            }
        }

        // Enforce cooldown if configured
        int days = Config.secondChanceCooldownDays;
        if (days > 0) {
            long now = self.level().getGameTime();
            CompoundTag persistent = player.getPersistentData();
            long nextAllowed = persistent.getLong(NBT_KEY);
            if (nextAllowed > now) {
                return; // on cooldown; do nothing
            }
        }

        float currentHealth = self.getHealth();
        float thresholdHealth = (float) (Math.max(0.0D, Config.secondChanceHealthThresholdHearts) * 2.0D);
        if (currentHealth <= thresholdHealth) return;

        float postHealth = currentHealth - amount;
        if (postHealth > 0.0f) return; // not lethal

        // Lethal blow detected; grant second chance
        float minHealthLeft = 1.0f; // half heart
        self.setHealth(minHealthLeft);

        int duration = 200; // 10 seconds
        if (Config.secondChanceResistanceEnabled) {
            int amp = Math.max(0, Config.secondChanceResistanceLevel - 1);
            self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amp));
        }
        if (Config.secondChanceNauseaEnabled) {
            int amp = Math.max(0, Config.secondChanceNauseaLevel - 1);
            self.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, amp));
        }

        // Set cooldown if configured
        if (days > 0) {
            long now = self.level().getGameTime();
            long nextAllowed = now + (long) days * 24000L;
            player.getPersistentData().putLong(NBT_KEY, nextAllowed);
        }

        if (Config.secondChanceToastEnabled && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SecondChanceToastPayload());
        }

        // Cancel the original hurt call to prevent death and damage processing
        cir.setReturnValue(true);
    }
}
