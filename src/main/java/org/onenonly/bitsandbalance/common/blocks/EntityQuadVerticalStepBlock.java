package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;

/** Quad vertical step with a passive, non-rendered block entity used for portable chunk NBT. */
public class EntityQuadVerticalStepBlock extends QuadVerticalStepBlock implements EntityBlock {
    public EntityQuadVerticalStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuadVerticalStepBlockEntity(pos, state);
    }
}
