package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.onenonly.bitsandbalance.fabric.combat.PowderSnowTags;
import org.onenonly.bitsandbalance.fabric.config.FabricCombatConfig;
import org.onenonly.bitsandbalance.fabric.network.SnowballPowderSnowPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Snowball Rework: apply configurable damage and freezing to hit entities.
 */
@Mixin(Snowball.class)
public abstract class SnowballMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$onHitEntity(EntityHitResult hit, CallbackInfo ci) {
        if (!FabricCombatConfig.enableSnowballRework) return;

        Snowball self = (Snowball) (Object) this;
        if (self.level().isClientSide()) return;

        Entity target = hit.getEntity();
        if (target == null) return;

        double baseDmg = Math.max(0.0D, FabricCombatConfig.snowballBaseDamage);
        double netherDmg = Math.max(0.0D, FabricCombatConfig.snowballNetherDamage);
        double dmg = baseDmg;

        if (isNetherNative(target.getType(), FabricCombatConfig.snowballNetherEntities)) {
            dmg = netherDmg;
        }

        DamageSource source = self.damageSources().thrown(self, self.getOwner());
        target.hurt(source, (float) dmg);

        if (target instanceof LivingEntity living) {
            int minSec = Math.max(0, FabricCombatConfig.snowballMinFreezeSeconds);
            int maxSec = Math.max(0, FabricCombatConfig.snowballMaxFreezeSeconds);
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
                if (t < 0) t = 0;
                if (t > 1) t = 1;
                double seconds = minSec + t * (maxSec - minSec);
                freezeTicks = (int) Math.round(seconds * 20.0);
            }

            if (freezeTicks > 0) {
                int current = living.getTicksFrozen();
                int cap = living.getTicksRequiredToFreeze();
                int next = Math.max(current, cap);
                living.setTicksFrozen(next);

                try {
                    target.hurt(self.damageSources().freeze(), 0.5F);
                } catch (Throwable ignored) {
                }

                long now = self.level().getGameTime();
                PowderSnowTags.extendUntil(living, now + (long) freezeTicks);

                for (ServerPlayer sp : PlayerLookup.tracking(living)) {
                    ServerPlayNetworking.send(sp, new SnowballPowderSnowPayload(living.getId(), freezeTicks));
                }
                if (living instanceof ServerPlayer sp) {
                    // ensure the hit player also receives it
                    ServerPlayNetworking.send(sp, new SnowballPowderSnowPayload(living.getId(), freezeTicks));
                }
            }
        }

        self.discard();
        ci.cancel();
    }

    private static boolean isNetherNative(EntityType<?> type, Set<String> netherEntityIds) {
        if (netherEntityIds == null || netherEntityIds.isEmpty()) {
            return false;
        }
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key != null && netherEntityIds.contains(key.toString());
    }
}
