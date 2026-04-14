package org.onenonly.bitsandbalance.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.util.List;

/**
 * Custom wood block that properly handles drops and tool effectiveness.
 */
public class AzaleaWoodBlock extends Block {
    
    public AzaleaWoodBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Always drop the block as an item
        return List.of(new ItemStack(this));
    }
}
