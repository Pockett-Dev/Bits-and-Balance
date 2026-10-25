package org.onenonly.bitsandbalance.fabric.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Instant effect that randomly teleports the affected entity within a radius.
 */
public final class DisplacementEffect extends MobEffect {
    private static final int TELEPORT_RADIUS = 100;
    private static final int MAX_ATTEMPTS = 50;

    public DisplacementEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        BlockPos start = entity.blockPosition();
        BlockPos target = findSafeTeleportLocation(level, start, level.getRandom());
        if (target == null) return true;

        entity.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        return true;
    }

    private static BlockPos findSafeTeleportLocation(ServerLevel level, BlockPos start, RandomSource random) {
        int startX = start.getX();
        int startZ = start.getZ();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int offsetX = random.nextInt(TELEPORT_RADIUS * 2 + 1) - TELEPORT_RADIUS;
            int offsetZ = random.nextInt(TELEPORT_RADIUS * 2 + 1) - TELEPORT_RADIUS;

            int x = startX + offsetX;
            int z = startZ + offsetZ;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafe(level, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean isSafe(Level level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState ground = level.getBlockState(pos.below());

        boolean feetClear = feet.getCollisionShape(level, pos).isEmpty();
        boolean headClear = head.getCollisionShape(level, pos.above()).isEmpty();
        boolean groundSolid = !ground.getCollisionShape(level, pos.below()).isEmpty();

        return feetClear && headClear && groundSolid;
    }
}
