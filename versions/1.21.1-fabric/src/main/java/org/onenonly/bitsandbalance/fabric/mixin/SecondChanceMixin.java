package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.combat.SecondChanceTags;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Second Chance: prevent unfair one-shots above the configured health threshold.
 * Leaves player at half a heart and applies effects for 10s.
 */
@Mixin(LivingEntity.class)
public abstract class SecondChanceMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-second-chance");

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$secondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$trySecondChance(source, amount, cir);
    }

    private void bitsandbalance$trySecondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricCombatConfig.enableSecondChance) return;

        LivingEntity living = (LivingEntity) (Object) this;
        if (living.level().isClientSide()) return;
        if (!(living instanceof Player player)) return;

        // Need a ServerLevel for gameTime (cooldown)
        ServerLevel serverLevel;
        try {
            serverLevel = (ServerLevel) living.level();
        } catch (Throwable ignored) {
            return;
        }

        if (FabricCombatConfig.secondChanceExcludeFallDamage) {
            try {
                if (source.is(DamageTypes.FALL)) {
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        float currentHealth = living.getHealth();

        float thresholdHealth = (float) (Math.max(0.0D, FabricCombatConfig.secondChanceHealthThresholdHearts) * 2.0D);
        if (currentHealth <= thresholdHealth) {
            // This is the most common "it doesn't work" case during testing.
            // Log only when the incoming hit would be lethal by simple subtraction.
            if (currentHealth - amount <= 0.0f) {
                LOGGER.info("Second Chance blocked: health threshold (health={}, threshold={}, max={}, amount={}, source={})", currentHealth, thresholdHealth, living.getMaxHealth(), amount, source);
            }
            return;
        }

        float postHealth = currentHealth - amount;
        if (postHealth > 0.0f) return;

        int days = Math.max(0, FabricCombatConfig.secondChanceCooldownDays);
        if (days > 0) {
            long now = serverLevel.getGameTime();
            long nextAllowed = SecondChanceTags.getNextAllowed(player);
            if (nextAllowed > now) {
                LOGGER.info("Second Chance blocked: cooldown (now={}, nextAllowed={}, days={}, source={})", now, nextAllowed, days, source);
                return;
            }
        }

        LOGGER.info("Second Chance TRIGGERED (health={}, max={}, amount={}, source={}, days={})", currentHealth, living.getMaxHealth(), amount, source, days);

        living.setHealth(1.0f);

        int duration = 200;
        if (FabricCombatConfig.secondChanceResistanceEnabled) {
            int amp = Math.max(0, FabricCombatConfig.secondChanceResistanceLevel - 1);
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amp));
        }
        if (FabricCombatConfig.secondChanceNauseaEnabled) {
            int amp = Math.max(0, FabricCombatConfig.secondChanceNauseaLevel - 1);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, amp));
        }

        if (days > 0) {
            long now = serverLevel.getGameTime();
            long nextAllowed = now + (long) days * 24000L;
            SecondChanceTags.setNextAllowed(player, nextAllowed);
        }

        cir.setReturnValue(true);
    }
}
