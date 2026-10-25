package org.onenonly.bitsandbalance.tweaks;


import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.onenonly.bitsandbalance.BitsAndBalance;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.world.phys.AABB;
import java.util.List;
import org.onenonly.bitsandbalance.Config;

/**
 * Tweaks: Despawn With Master
 * - When an Evoker is removed from the world, terminate its Vex as if by a player (if enabled).
 * - When a Shulker is removed from the world, terminate its Shulker Bullets as if by a player (if enabled).
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class DespawnWithMasterEvents {

    // Handle true removal from a level (includes death, despawn, teleport between levels, etc.)
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level == null || level.isClientSide()) return;

        if (entity instanceof Evoker evoker) {
            if (Config.despawnVexWithEvoker) {
                tryTerminateVex(level, evoker);
            }
        } else if (entity instanceof Shulker shulker) {
            if (Config.despawnBulletsWithShulker) {
                tryTerminateShulkerBullets(level, shulker);
            }
        }
    }

    // Redundant safety: also handle explicit death of living entities
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity le = event.getEntity();
        Level level = le.level();
        if (level == null || level.isClientSide()) return;

        if (le instanceof Evoker evoker) {
            if (Config.despawnVexWithEvoker) {
                tryTerminateVex(level, evoker);
            }
        } else if (le instanceof Shulker shulker) {
            if (Config.despawnBulletsWithShulker) {
                tryTerminateShulkerBullets(level, shulker);
            }
        }
    }

    private static void tryTerminateVex(Level level, LivingEntity owner) {
        try {
            // Search a generous radius around the owner; Vex stay near their summoner.
            AABB box = owner.getBoundingBox().inflate(256.0);
            List<Vex> vexes = level.getEntitiesOfClass(Vex.class, box, v -> v != null && !v.isRemoved());
            for (Vex vex : vexes) {
                try {
                    Entity vexOwner = vex.getOwner();
                    if (vexOwner != null && vexOwner.getUUID().equals(owner.getUUID())) {
                        DamageSource src = selectPlayerAttributedSource(level, owner);
                        // Apply overwhelming damage to ensure death and proper drops/xp
                        vex.hurt(src, Float.MAX_VALUE);
                        // Fallback: if somehow still not removed, mark as killed
                        if (!vex.isRemoved() && level instanceof net.minecraft.server.level.ServerLevel sl) vex.kill(sl);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
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
                        // Try to damage the projectile with a player-attributed source first
                        bullet.hurt(src, Float.MAX_VALUE);
                        // If not removed by hurt handling, explicitly kill it (marks as killed rather than discarded)
                        if (!bullet.isRemoved() && level instanceof net.minecraft.server.level.ServerLevel sl) bullet.kill(sl);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static DamageSource selectPlayerAttributedSource(Level level, LivingEntity owner) {
        try {
            Player nearest = level.getNearestPlayer(owner, 128.0);
            if (nearest != null) {
                return level.damageSources().playerAttack(nearest);
            }
            // Attribute to the owner mob if available; else use generic kill
            return owner != null ? level.damageSources().mobAttack(owner) : level.damageSources().genericKill();
        } catch (Throwable t) {
            try { return level.damageSources().genericKill(); } catch (Throwable ignored) { return null; }
        }
    }
}
