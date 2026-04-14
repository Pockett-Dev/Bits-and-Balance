package org.onenonly.bitsandbalance.blocks;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Custom trapdoor block that properly handles drops.
 */
public class AzaleaTrapDoorBlock extends TrapDoorBlock {
    
    public AzaleaTrapDoorBlock(BlockSetType blockSetType, BlockBehaviour.Properties properties) {
        super(blockSetType, properties);
    }
    
    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Always drop the block as an item
        return List.of(new net.minecraft.world.item.ItemStack(this));
    }
}
