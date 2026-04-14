package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class AzaleaButtonBlock extends ButtonBlock {
    public AzaleaButtonBlock(BlockSetType blockSetType, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        super(blockSetType, ticksToStayPressed, properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(this));
    }
}
