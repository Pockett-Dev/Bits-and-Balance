package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class AzaleaStairBlock extends StairBlock {
    public AzaleaStairBlock(BlockState baseBlockState, BlockBehaviour.Properties properties) {
        super(baseBlockState, properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
