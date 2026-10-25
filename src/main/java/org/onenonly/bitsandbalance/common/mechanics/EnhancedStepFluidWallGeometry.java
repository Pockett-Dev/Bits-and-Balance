package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;

/** Fluid-only wall rules that cannot be represented by Minecraft's block-granular water state. */
public final class EnhancedStepFluidWallGeometry {
    private EnhancedStepFluidWallGeometry() {
    }

    public static boolean blocksAxisFlow(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!(state.getBlock() instanceof QuadStepBlock)
                || face == null
                || !face.getAxis().isHorizontal()
                || face.getAxis() != state.getValue(QuadStepBlock.AXIS)) {
            return false;
        }

        BlockState[] slots = bitsandbalance$getSlots(level, pos);
        if (slots == null) {
            return false;
        }

        int occupied = 0;
        for (int index = 0; index < 4; index++) {
            if (bitsandbalance$slot(slots, index) != null) {
                occupied++;
            }
        }
        if (occupied != 2) {
            return false;
        }

        return (bitsandbalance$slot(slots, 0) != null && bitsandbalance$slot(slots, 2) != null)
                || (bitsandbalance$slot(slots, 1) != null && bitsandbalance$slot(slots, 3) != null);
    }

    private static BlockState[] bitsandbalance$getSlots(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof QuadStepBlockEntity quad) {
            return quad.getAllSlabs();
        }
        return level instanceof Level actualLevel
                ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong())
                : null;
    }

    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return index >= 0 && index < slots.length ? slots[index] : null;
    }
}
