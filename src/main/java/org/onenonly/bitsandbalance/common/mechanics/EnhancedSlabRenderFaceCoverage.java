package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;

/** Full boundary-face coverage for render snapshots, without gameplay attachment exceptions. */
public final class EnhancedSlabRenderFaceCoverage {
    private EnhancedSlabRenderFaceCoverage() {
    }

    public static boolean isFull(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        boolean horizontal = state.getBlock() instanceof QuadStepBlock;
        if (!horizontal && !(state.getBlock() instanceof QuadVerticalStepBlock)) {
            return SupportType.FULL.isSupporting(state, level, pos, face);
        }

        BlockEntity entity = level.getBlockEntity(pos);
        EnhancedSlabMaterialSavedData.MaterialView stored = EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
        BlockState[] contraption = entity == null && stored == null
                ? EnhancedSlabCreateCompat.getContraptionSlots(level, pos, state) : null;
        int mask = 0;
        for (int slot = 0; slot < 4; slot++) {
            BlockState material = entity instanceof QuadStepBlockEntity quad ? quad.getSlabAt(slot)
                    : entity instanceof QuadVerticalStepBlockEntity quad ? quad.getSlabAt(slot)
                    : stored != null ? stored.slot(slot)
                    : contraption != null && slot < contraption.length ? contraption[slot] : null;
            if (material != null) mask |= 1 << slot;
        }
        // Storage-backed containers have no BE in render snapshots; their vanilla support
        // shape falls back to a cube. Query their actual slots instead of that fallback.
        int required;
        if (horizontal) {
            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            required = switch (face) {
                case DOWN -> 3;
                case UP -> 12;
                case WEST -> axis == Direction.Axis.X ? 5 : 15;
                case EAST -> axis == Direction.Axis.X ? 10 : 15;
                case NORTH -> axis == Direction.Axis.Z ? 5 : 15;
                case SOUTH -> axis == Direction.Axis.Z ? 10 : 15;
            };
        } else {
            required = switch (face) {
                case DOWN, UP -> 15;
                case WEST -> 5;
                case EAST -> 10;
                case NORTH -> 3;
                case SOUTH -> 12;
            };
        }
        return (mask & required) == required;
    }
}
