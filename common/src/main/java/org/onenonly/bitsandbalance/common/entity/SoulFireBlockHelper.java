package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;

public final class SoulFireBlockHelper {
    private SoulFireBlockHelper() {
    }

    public static AABB fireCheckBox(AABB box) {
        return box.inflate(0.001D);
    }

    public static boolean isTouchingSoulFire(Level level, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY) - 1;
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.getBlock() == Blocks.SOUL_FIRE) {
                        return true;
                    }
                    if (state.getBlock() == Blocks.SOUL_CAMPFIRE && (!state.hasProperty(CampfireBlock.LIT) || state.getValue(CampfireBlock.LIT))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isTouchingNormalFire(Level level, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY) - 1;
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.getBlock() == Blocks.FIRE) {
                        return true;
                    }
                    if (state.getBlock() == Blocks.CAMPFIRE && (!state.hasProperty(CampfireBlock.LIT) || state.getValue(CampfireBlock.LIT))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}