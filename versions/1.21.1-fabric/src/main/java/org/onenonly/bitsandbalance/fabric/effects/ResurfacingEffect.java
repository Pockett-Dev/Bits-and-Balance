package org.onenonly.bitsandbalance.fabric.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import org.onenonly.bitsandbalance.common.mechanics.LeashedTeleportFollowers;
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;

/**
 * Instant effect that teleports the affected entity to the surface at its current X/Z.
 */
public final class ResurfacingEffect extends MobEffect {
    public ResurfacingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x008080);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!PotionFeatureFlags.isResurfacingPotionEnabled()) {
            return true;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }

        int x = entity.blockPosition().getX();
        int z = entity.blockPosition().getZ();

        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos target = new BlockPos(x, topY + 1, z);

        List<UUID> followerIds = entity instanceof ServerPlayer serverPlayer
            ? LeashedTeleportFollowers.captureLeashedFollowerIds(serverPlayer)
            : List.of();

        entity.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        if (entity instanceof ServerPlayer serverPlayer && !followerIds.isEmpty()) {
            LeashedTeleportFollowers.teleportCapturedFollowers(
                serverPlayer,
                followerIds,
                new Vec3(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D)
            );
        }
        return true;
    }
}
