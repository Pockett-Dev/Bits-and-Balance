package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;

public record EnhancedSlabPistonData(Kind kind, BlockState[] slabs) {

    public enum Kind {
        VERTICAL_SLAB,
        STEP,
        VERTICAL_STEP,
        QUAD_STEP,
        QUAD_VERTICAL_STEP
    }

    public EnhancedSlabPistonData {
        slabs = slabs != null ? slabs.clone() : new BlockState[0];
    }

    public static @Nullable EnhancedSlabPistonData capture(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new EnhancedSlabPistonData(Kind.VERTICAL_SLAB, new BlockState[] {
                    verticalSlab.getFacingSlab(),
                    verticalSlab.getOppositeSlab()
            });
        }
        if (blockEntity instanceof StepBlockEntity step) {
            BlockState slab = step.getSlabState();
            return slab != null ? new EnhancedSlabPistonData(Kind.STEP, new BlockState[] { slab }) : null;
        }
        if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            BlockState slab = verticalStep.getSlabState();
            return slab != null ? new EnhancedSlabPistonData(Kind.VERTICAL_STEP, new BlockState[] { slab }) : null;
        }
        if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            return new EnhancedSlabPistonData(Kind.QUAD_STEP, quadStep.getAllSlabs());
        }
        if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return new EnhancedSlabPistonData(Kind.QUAD_VERTICAL_STEP, quadVerticalStep.getAllSlabs());
        }
        return null;
    }

    public @Nullable BlockState slab(int index) {
        return index >= 0 && index < slabs.length ? slabs[index] : null;
    }

    public BlockState[] copySlabs() {
        return slabs.clone();
    }
}