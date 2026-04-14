package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.network.SnowballPowderSnowPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.onenonly.bitsandbalance.Config;
/**
 * Snowball Rework: apply configurable damage and freezing to hit entities.
 */
@Mixin(Snowball.class)
public abstract class SnowballMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void rebalance$onHitEntity(EntityHitResult hit, CallbackInfo ci) {
        if (!Config.enableSnowballRework) return;

        Snowball self = (Snowball) (Object) this;
        if (self.level().isClientSide()) return;

        Entity target = hit.getEntity();
        if (target == null) return;

        // Determine damage based on configured Nether-native entity types
        double baseDmg = Math.max(0.0D, Config.snowballBaseDamage);
        double netherDmg = Math.max(0.0D, Config.snowballNetherDamage);
        double dmg = baseDmg;
        EntityType<?> type = target.getType();
        if (Config.snowballNetherEntityTypes != null && Config.snowballNetherEntityTypes.contains(type)) {
            dmg = netherDmg;
        }

        // Apply damage to any entity that can be damaged
        DamageSource source = self.damageSources().thrown(self, self.getOwner());
        target.hurt(source, (float) dmg);

        // Apply freezing only to living entities
        if (target instanceof LivingEntity living) {
            int minSec = Math.max(0, Config.snowballMinFreezeSeconds);
            int maxSec = Math.max(0, Config.snowballMaxFreezeSeconds);
            if (maxSec < minSec) {
                int tmp = minSec;
                minSec = maxSec;
                maxSec = tmp;
            }

            int freezeTicks;
            double minD = Math.min(baseDmg, netherDmg);
            double maxD = Math.max(baseDmg, netherDmg);
            if (maxD <= minD + 1.0E-6) {
                freezeTicks = minSec * 20;
            } else {
                double t = (dmg - minD) / (maxD - minD);
                if (t < 0) t = 0; if (t > 1) t = 1;
                double seconds = minSec + t * (maxSec - minSec);
                freezeTicks = (int) Math.round(seconds * 20.0);
            }

            if (freezeTicks > 0) {
                int current = living.getTicksFrozen();
                int cap = living.getTicksRequiredToFreeze();
                int next = Math.max(current, cap);
                living.setTicksFrozen(next);
                // Apply an immediate FREEZE damage tick to simulate ~5s of prior exposure
                try {
                    target.hurt(self.damageSources().freeze(), 0.5F);
                } catch (Throwable ignored) {
                }
                // Persist marking for subsequent ticks equal to computed freeze duration
                if (freezeTicks > 0) {
                    long now = self.level().getGameTime();
                    long existing = living.getPersistentData().getLong("bitsandbalance_powdersnow_mark_until").orElse(0L);
                    long proposed = now + (long) freezeTicks;
                    long until = Math.max(existing, proposed);
                    living.getPersistentData().putLong("bitsandbalance_powdersnow_mark_until", until);
                    // Notify clients to render Powder Snow visuals for at least the new duration (client will also extend if already longer)
                    PacketDistributor.sendToPlayersTrackingEntity(living, new SnowballPowderSnowPayload(living.getId(), freezeTicks));
                }
            }
        }

        // Discard the snowball like vanilla and prevent further processing
        self.discard();
        ci.cancel();
    }
}
