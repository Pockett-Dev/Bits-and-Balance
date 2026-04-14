package org.onenonly.bitsandbalance.blocks;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Custom fence gate block that properly handles drops.
 */
public class AzaleaFenceGateBlock extends FenceGateBlock {
    
    public AzaleaFenceGateBlock(WoodType woodType, BlockBehaviour.Properties properties) {
        super(woodType, properties);
    }
    
    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Always drop the block as an item
        return List.of(new net.minecraft.world.item.ItemStack(this));
    }
}
