package org.onenonly.bitsandbalance.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.registry.ModBlockEntities;

/**
 * Custom hanging sign block entity for Azalea wood type.
 * This extends HangingSignBlockEntity and overrides getType() to return our custom block entity type.
 */
public class AzaleaHangingSignBlockEntity extends HangingSignBlockEntity {
    
    public AzaleaHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }
    
    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return ModBlockEntities.AZALEA_HANGING_SIGN.get();
    }
}
