package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Invalidates water flow after a block-entity-backed enhanced slab changes its occupied shape.
 */
public final class EnhancedSlabFluidGeometryRefresh {
    private EnhancedSlabFluidGeometryRefresh() {
    }

    public static void afterStoredShapeChange(@Nullable Level level, BlockPos pos, BlockState ownerState) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState current = serverLevel.getBlockState(pos);
        if (current.getBlock() != ownerState.getBlock() || !bitsandbalance$hasNearbyWater(serverLevel, pos)) {
            return;
        }

        serverLevel.updateNeighborsAt(pos, current.getBlock());
        bitsandbalance$scheduleWaterTick(serverLevel, pos);
        for (Direction direction : Direction.values()) {
            bitsandbalance$scheduleWaterTick(serverLevel, pos.relative(direction));
        }
    }

    private static boolean bitsandbalance$hasNearbyWater(ServerLevel level, BlockPos pos) {
        if (bitsandbalance$isWater(level.getFluidState(pos))) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (bitsandbalance$isWater(level.getFluidState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static void bitsandbalance$scheduleWaterTick(ServerLevel level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (bitsandbalance$isWater(fluidState)) {
            level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
        }
    }

    private static boolean bitsandbalance$isWater(FluidState fluidState) {
        return fluidState.getType() == Fluids.WATER || fluidState.getType() == Fluids.FLOWING_WATER;
    }
}
