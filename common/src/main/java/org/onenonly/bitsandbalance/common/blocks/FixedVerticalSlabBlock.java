package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;

/**
 * A per-slab vertical slab block (unique registry ID per slab {@link Block}).
 *
 * <p>Behavior is identical to {@link VerticalSlabBlock}, but the block itself encodes which slab block it represents.
 */
public class FixedVerticalSlabBlock extends VerticalSlabBlock {

    private final Block sourceSlab;

    public FixedVerticalSlabBlock(Properties properties, Block sourceSlab) {
        super(properties);
        this.sourceSlab = sourceSlab;
    }

    public Block getSourceSlab() {
        return sourceSlab;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Ensure the block entity starts with the correct slab for stable rendering even before the
        // placement mixin has a chance to set the slabs explicitly.
        VerticalSlabBlockEntity be = new VerticalSlabBlockEntity(pos, state);
        try {
            be.setSlabs(sourceSlab.defaultBlockState(), sourceSlab.defaultBlockState());
        } catch (Throwable ignored) {
        }
        return be;
    }
}
