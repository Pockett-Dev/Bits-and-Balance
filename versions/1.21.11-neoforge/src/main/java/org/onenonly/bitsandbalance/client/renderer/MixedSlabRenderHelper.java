package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;

import java.util.List;

public final class MixedSlabRenderHelper {
    private MixedSlabRenderHelper() {
    }

    public static void renderTerrain(BlockRenderDispatcher dispatcher,
                                     BlockAndTintGetter level,
                                     net.minecraft.core.BlockPos blockPos,
                                     BlockState bottomSlabState,
                                     BlockState topSlabState,
                                     PoseStack poseStack,
                                     VertexConsumer consumer) {
        if (bottomSlabState != null) {
            renderTerrainSlabHalf(dispatcher, level, blockPos, bottomSlabState, poseStack, consumer);
        }
        if (topSlabState != null) {
            renderTerrainSlabHalf(dispatcher, level, blockPos, topSlabState, poseStack, consumer);
        }
    }

    private static void renderTerrainSlabHalf(BlockRenderDispatcher dispatcher,
                                              BlockAndTintGetter level,
                                              net.minecraft.core.BlockPos blockPos,
                                              BlockState slabState,
                                              PoseStack poseStack,
                                              VertexConsumer consumer) {
        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(slabState);
        long seed = blockPos.asLong();
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(seed));
        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, slabState, blockPos, level, poseStack, consumer, parts);
        } finally {
            BlockColorRenderContext.clear();
        }
    }
}
