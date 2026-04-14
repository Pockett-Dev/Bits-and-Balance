package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Lightweight proxy block used only to supply geometry for vertical-step crack overlays.
 */
public class VerticalStepCrackProxyBlock extends Block {

    public static final EnumProperty<Direction> FACING =
            EnumProperty.create("facing", Direction.class, Direction.NORTH, Direction.SOUTH);
    public static final EnumProperty<Direction> SIDE =
            EnumProperty.create("side", Direction.class, Direction.EAST, Direction.WEST);

    public VerticalStepCrackProxyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, Direction.EAST));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}