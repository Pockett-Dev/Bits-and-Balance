package org.onenonly.bitsandbalance.blocks;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Custom button block that properly handles drops.
 */
public class AzaleaButtonBlock extends ButtonBlock {
    
    public AzaleaButtonBlock(BlockSetType blockSetType, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        super(blockSetType, ticksToStayPressed, properties);
    }
    
    @Override
    public List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Always drop the block as an item
        return List.of(new net.minecraft.world.item.ItemStack(this));
    }
}
