package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;

/**
 * Block-entity-backed step used only for generic or legacy single-step storage.
 * Fixed generated step variants deliberately extend {@link StepBlock} directly.
 */
public class EntityStepBlock extends StepBlock implements EntityBlock {
    public EntityStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StepBlockEntity(pos, state);
    }
}
