package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;

/** Quad step with a passive, non-rendered block entity used for portable chunk NBT. */
public class EntityQuadStepBlock extends QuadStepBlock implements EntityBlock {
    public EntityQuadStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuadStepBlockEntity(pos, state);
    }
}
