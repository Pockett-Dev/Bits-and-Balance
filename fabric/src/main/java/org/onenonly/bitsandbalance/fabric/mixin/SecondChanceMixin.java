package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Second Chance: prevent unfair one-shots when player has >7.5 hearts.
 * Leaves player at half a heart and applies effects for 10s.
 */
@Mixin(Entity.class)
public abstract class SecondChanceMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-second-chance");

    @Inject(method = "hurtOrSimulate", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$secondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$trySecondChance(source, amount, cir);
    }

    private void bitsandbalance$trySecondChance(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricCombatConfig.enableSecondChance) return;

        Entity selfEntity = (Entity) (Object) this;
        if (selfEntity.level().isClientSide()) return;
        if (!(selfEntity instanceof Player player)) return;
        if (!(selfEntity instanceof LivingEntity living)) return;

        // Need a ServerLevel for gameTime (cooldown)
        ServerLevel serverLevel;
        try {
            serverLevel = (ServerLevel) selfEntity.level();
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

        // Only apply if player currently has more than 7.5 hearts (15.0 health)
        if (currentHealth <= 15.0f) {
            // This is the most common "it doesn't work" case during testing.
            // Log only when the incoming hit would be lethal by simple subtraction.
            if (currentHealth - amount <= 0.0f) {
                LOGGER.info("Second Chance blocked: health threshold (health={}, max={}, amount={}, source={})", currentHealth, living.getMaxHealth(), amount, source);
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
            living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amp));
        }
        if (FabricCombatConfig.secondChanceNauseaEnabled) {
            int amp = Math.max(0, FabricCombatConfig.secondChanceNauseaLevel - 1);
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, amp));
        }

        if (days > 0) {
            long now = serverLevel.getGameTime();
            long nextAllowed = now + (long) days * 24000L;
            SecondChanceTags.setNextAllowed(player, nextAllowed);
        }

        cir.setReturnValue(true);
    }
}
