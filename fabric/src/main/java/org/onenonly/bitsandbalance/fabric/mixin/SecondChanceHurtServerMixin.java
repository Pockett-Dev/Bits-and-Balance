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
 * Second Chance hook for direct server damage calls.
 *
 * Some server-side callers (notably kill/commands) call hurtServer directly,
 * bypassing Entity.hurtOrSimulate(). This mirrors the NeoForge mixin's
 * additional hurtServer injection.
 */
@Mixin(Player.class)
public abstract class SecondChanceHurtServerMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-second-chance");

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$secondChanceHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricCombatConfig.enableSecondChance) return;

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        if (FabricCombatConfig.secondChanceExcludeFallDamage) {
            try {
                if (source.is(DamageTypes.FALL)) {
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        int days = Math.max(0, FabricCombatConfig.secondChanceCooldownDays);
        LivingEntity living = (LivingEntity) player;

        float currentHealth = living.getHealth();
        if (currentHealth <= 15.0f) {
            if (currentHealth - amount <= 0.0f) {
                LOGGER.info("Second Chance blocked (hurtServer): health threshold (health={}, max={}, amount={}, source={})", currentHealth, living.getMaxHealth(), amount, source);
            }
            return;
        }

        float postHealth = currentHealth - amount;
        if (postHealth > 0.0f) return;

        if (days > 0) {
            long now = level.getGameTime();
            long nextAllowed = SecondChanceTags.getNextAllowed(player);
            if (nextAllowed > now) {
                LOGGER.info("Second Chance blocked (hurtServer): cooldown (now={}, nextAllowed={}, days={}, source={})", now, nextAllowed, days, source);
                return;
            }
        }

        LOGGER.info("Second Chance TRIGGERED (hurtServer) (health={}, max={}, amount={}, source={}, days={})", currentHealth, living.getMaxHealth(), amount, source, days);

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
            long now = level.getGameTime();
            long nextAllowed = now + (long) days * 24000L;
            SecondChanceTags.setNextAllowed(player, nextAllowed);
        }

        cir.setReturnValue(true);
    }
}
