package org.onenonly.bitsandbalance.fabric.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Instant effect that teleports the affected entity to the surface at its current X/Z.
 *
 * Fabric port note: NeoForge version also handled leashed followers + config toggles.
 * Those systems are not yet ported, so this is intentionally minimal.
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
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        int x = entity.blockPosition().getX();
        int z = entity.blockPosition().getZ();

        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos target = new BlockPos(x, topY + 1, z);

        entity.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        return true;
    }
}
