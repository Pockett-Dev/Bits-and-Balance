package org.onenonly.bitsandbalance.client.renderer;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class MovingBlockRenderTypeCompat {
    private MovingBlockRenderTypeCompat() {
    }

    public static RenderType get(BlockState blockState, BlockStateModelPart part) {
        return getForLayer(blockState, findLayer(part));
    }

    public static RenderType get(BlockState blockState, List<BlockStateModelPart> parts) {
        return getForLayer(blockState, findLayer(parts));
    }

    private static RenderType getForLayer(BlockState blockState, ChunkSectionLayer layer) {
        if (layer == ChunkSectionLayer.TRANSLUCENT || (layer == null && blockState != null && !blockState.getFluidState().isEmpty())) {
            return RenderTypes.translucentMovingBlock();
        }
        if (layer == ChunkSectionLayer.CUTOUT) {
            return RenderTypes.cutoutMovingBlock();
        }
        return RenderTypes.solidMovingBlock();
    }

    private static ChunkSectionLayer findLayer(List<BlockStateModelPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        for (BlockStateModelPart part : parts) {
            ChunkSectionLayer layer = findLayer(part);
            if (layer != null) {
                return layer;
            }
        }

        return null;
    }

    private static ChunkSectionLayer findLayer(BlockStateModelPart part) {
        if (part == null) {
            return null;
        }

        List<BakedQuad> all = part.getQuads(null);
        ChunkSectionLayer layer = firstLayer(all);
        if (layer != null) {
            return layer;
        }

        for (Direction direction : Direction.values()) {
            layer = firstLayer(part.getQuads(direction));
            if (layer != null) {
                return layer;
            }
        }

        return null;
    }

    private static ChunkSectionLayer firstLayer(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        for (BakedQuad quad : quads) {
            if (quad != null && quad.materialInfo() != null) {
                return quad.materialInfo().layer();
            }
        }
        return null;
    }
}