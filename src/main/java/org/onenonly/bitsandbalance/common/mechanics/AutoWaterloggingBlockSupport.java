package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class AutoWaterloggingBlockSupport {
    private AutoWaterloggingBlockSupport() {
    }

    public static void addWaterloggedProperty(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    public static boolean isWaterlogged(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    public static boolean isAddedWaterloggingBlockFamily(Block block) {
        if (block instanceof DoorBlock) {
            return true;
        }
        if (block instanceof FenceGateBlock) {
            return true;
        }
        return block instanceof BasePressurePlateBlock;
    }

    public static BlockState asDry(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                : state;
    }

    public static FluidState getFluidState(BlockState state) {
        return isWaterlogged(state) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    public static BlockState preserveWaterloggedUpdateShape(BlockState original, BlockState updated) {
        if (!isAddedWaterloggingBlockFamily(original.getBlock()) || !isWaterlogged(original)) {
            return updated;
        }
        if (updated.isAir()) {
            return Blocks.WATER.defaultBlockState();
        }
        if (updated.getBlock() == original.getBlock()
                && updated.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return updated.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return updated;
    }
}
