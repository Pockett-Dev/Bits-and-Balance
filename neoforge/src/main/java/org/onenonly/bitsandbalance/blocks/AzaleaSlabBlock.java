package org.onenonly.bitsandbalance.blocks;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Custom slab block that properly handles drops.
 */
public class AzaleaSlabBlock extends SlabBlock {
    
    public AzaleaSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Always drop the block as an item
        return List.of(new net.minecraft.world.item.ItemStack(this));
    }
}
