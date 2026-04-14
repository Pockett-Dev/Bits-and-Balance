package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.blocks.entity.AzaleaHangingSignBlockEntity;

/**
 * Azalea wall hanging sign that uses our registered block entity type.
 */
public class AzaleaWallHangingSignBlock extends WallHangingSignBlock {
    
    public AzaleaWallHangingSignBlock(WoodType woodType, Properties properties) {
        super(woodType, properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AzaleaHangingSignBlockEntity(pos, state);
    }
}
