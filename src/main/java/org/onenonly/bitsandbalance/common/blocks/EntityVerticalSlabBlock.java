package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;

/**
 * Block-entity-backed vertical slab used only when slab material cannot be encoded by the block ID.
 * Fixed generated vertical slabs extend {@link VerticalSlabBlock} directly.
 */
public class EntityVerticalSlabBlock extends VerticalSlabBlock implements EntityBlock {
    public EntityVerticalSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VerticalSlabBlockEntity(pos, state);
    }
}
