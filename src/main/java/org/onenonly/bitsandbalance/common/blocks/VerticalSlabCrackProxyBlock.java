package org.onenonly.bitsandbalance.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;

/**
 * Lightweight proxy block used <strong>only</strong> to supply geometry for the
 * vertical-slab block-breaking crack animation.
 *
 * <p>Vertical slabs are rendered by a block entity renderer (RenderShape.INVISIBLE),
 * so {@code BlockRenderDispatcher.renderBreakingTexture} skips them.  By substituting
 * a state from this proxy block (which returns RenderShape.MODEL) in the
 * {@code LevelRendererEnhancedSlabCrumblingMixin}, the vanilla crumbling pipeline
 * can look up the correct half-prism geometry models and draw the crack overlay.
 *
 * <p>This block is never placed in the world and has no item form.
 */
public class VerticalSlabCrackProxyBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public VerticalSlabCrackProxyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Must be MODEL so that renderBreakingTexture renders it.
        return RenderShape.MODEL;
    }
}
