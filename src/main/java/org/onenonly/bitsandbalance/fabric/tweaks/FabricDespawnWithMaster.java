package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.List;

/**
 * Fabric port: Despawn With Master
 * When an Evoker/Shulker is removed, kill its Vex/Bullets (not discard), attributing the kill
 * to a nearby player when possible.
 */
public final class FabricDespawnWithMaster {
    private FabricDespawnWithMaster() {
    }

    public static void init() {
        // Handle entity unload (covers dimension changes, despawns, etc.)
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof Evoker evoker && FabricTweaksConfig.despawnVexWithEvoker) {
                tryTerminateVex(level, evoker);
            } else if (entity instanceof Shulker shulker && FabricTweaksConfig.despawnBulletsWithShulker) {
                tryTerminateShulkerBullets(level, shulker);
            }
        });
        
        // Also handle death as a backup
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            Level level = entity.level();
            if (level == null || level.isClientSide()) return;
            
            if (entity instanceof Evoker evoker && FabricTweaksConfig.despawnVexWithEvoker) {
                tryTerminateVex(level, evoker);
            } else if (entity instanceof Shulker shulker && FabricTweaksConfig.despawnBulletsWithShulker) {
                tryTerminateShulkerBullets(level, shulker);
            }
        });
    }

    private static void tryTerminateVex(Level level, LivingEntity owner) {
        try {
            AABB box = owner.getBoundingBox().inflate(256.0);
            List<Vex> vexes = level.getEntitiesOfClass(Vex.class, box, v -> v != null && !v.isRemoved());
            for (Vex vex : vexes) {
                try {
                    Entity vexOwner = vex.getOwner();
                    if (vexOwner != null && vexOwner.getUUID().equals(owner.getUUID())) {
                        DamageSource src = selectPlayerAttributedSource(level, owner);
                        vex.hurt(src, Float.MAX_VALUE);
                        if (!vex.isRemoved() && level instanceof ServerLevel sl) {
                            vex.kill(sl);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tryTerminateShulkerBullets(Level level, LivingEntity owner) {
        try {
            AABB box = owner.getBoundingBox().inflate(256.0);
            List<ShulkerBullet> bullets = level.getEntitiesOfClass(ShulkerBullet.class, box, b -> b != null && !b.isRemoved());
            for (ShulkerBullet bullet : bullets) {
                try {
                    Entity bOwner = bullet.getOwner();
                    if (bOwner != null && bOwner.getUUID().equals(owner.getUUID())) {
                        DamageSource src = selectPlayerAttributedSource(level, owner);
                        bullet.hurt(src, Float.MAX_VALUE);
                        if (!bullet.isRemoved() && level instanceof ServerLevel sl) {
                            bullet.kill(sl);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static DamageSource selectPlayerAttributedSource(Level level, LivingEntity owner) {
        try {
            Player nearest = level.getNearestPlayer(owner, 128.0);
            if (nearest != null) {
                return level.damageSources().playerAttack(nearest);
            }
            return owner != null ? level.damageSources().mobAttack(owner) : level.damageSources().genericKill();
        } catch (Throwable t) {
            try {
                return level.damageSources().genericKill();
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
