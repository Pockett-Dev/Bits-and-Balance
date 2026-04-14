package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Validation utilities for mechanics like Sitting.
 * Keep checks conservative; server is authoritative, but client can call too to gray out inputs.
 */
public final class Validation {
    private Validation() {}

    /**
     * Test whether the player can start or remain sitting.
     * Rules:
     * - Not a passenger, not sleeping, not swimming/crawling/fall-flying.
     * - On ground (or standing on a solid block), and not in water/lava.
     * - Adequate space so reduced-height bounding box does not collide.
     */
    public static boolean testSit(Player p) {
        if (p == null) return false;
        try {
            if (p.isRemoved() || p.isDeadOrDying()) return false;
        } catch (Throwable ignored) {}

        // Disallowed states
        try { if (p.isPassenger()) return false; } catch (Throwable ignored) {}
        try { if (p.isSleeping()) return false; } catch (Throwable ignored) {}
        try { if (p.isSwimming()) return false; } catch (Throwable ignored) {}
        try { if (p.isFallFlying()) return false; } catch (Throwable ignored) {}
        try { if (p.getPose() == Pose.SWIMMING) return false; } catch (Throwable ignored) {}
        try { if (p.isInLava() || p.isInWater()) return false; } catch (Throwable ignored) {}

        // If the mod tracks crawling intent in persistent data, ensure we are not crawling
        try { if (p.getPersistentData().getBoolean("bitsandbalance_crawling").orElse(false)) return false; } catch (Throwable ignored) {}

        // Must be on ground (safer transitions). Allow tiny tolerance on client.
        boolean onGround = false;
        try { onGround = p.onGround(); } catch (Throwable ignored) {}
        if (!onGround) {
            // Fallback ground check: check the block directly below feet is solid (server side mostly)
            try {
                BlockPos below = BlockPos.containing(p.getX(), Math.floor(p.getY() - 0.001), p.getZ()).below();
                BlockState state = p.level().getBlockState(below);
                if (state.isAir()) return false;
            } catch (Throwable t) {
                return false;
            }
        }

        // Collision space: simulate reducing height by ~0.5 blocks.
        try {
            AABB bb = p.getBoundingBox();
            double reduce = 0.5D; // tweak with dimension change
            AABB reduced = new AABB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + Math.max(0.3D, (bb.maxY - bb.minY) - reduce), bb.maxZ);
            // Ensure we do not push into blocks
            if (p.level() instanceof ServerLevel) {
                if (!((ServerLevel) p.level()).noCollision(p, reduced)) return false;
            } else {
                if (!p.level().noCollision(reduced)) return false;
            }
        } catch (Throwable ignored) {}

        return true;
    }
}
