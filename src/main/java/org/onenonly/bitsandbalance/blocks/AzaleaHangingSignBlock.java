package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.blocks.entity.AzaleaHangingSignBlockEntity;

/**
 * Azalea ceiling hanging sign that uses our registered block entity type.
 */
public class AzaleaHangingSignBlock extends CeilingHangingSignBlock {
    
    public AzaleaHangingSignBlock(WoodType woodType, Properties properties) {
        super(woodType, properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AzaleaHangingSignBlockEntity(pos, state);
    }
}
