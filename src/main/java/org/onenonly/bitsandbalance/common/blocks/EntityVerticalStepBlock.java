package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;

/**
 * Block-entity-backed vertical step used only for generic or legacy single-step storage.
 * Fixed generated vertical-step variants deliberately extend {@link VerticalStepBlock} directly.
 */
public class EntityVerticalStepBlock extends VerticalStepBlock implements EntityBlock {
    public EntityVerticalStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VerticalStepBlockEntity(pos, state);
    }
}
