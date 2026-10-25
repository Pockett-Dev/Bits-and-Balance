package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;

import java.util.List;

public final class MixedSlabRenderHelper {
    private MixedSlabRenderHelper() {
    }

    public static void renderTerrain(Object dispatcher,
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

    private static void renderTerrainSlabHalf(Object dispatcher,
                                              BlockAndTintGetter level,
                                              net.minecraft.core.BlockPos blockPos,
                                              BlockState slabState,
                                              PoseStack poseStack,
                                              VertexConsumer consumer) {
        List<BlockStateModelPart> parts = RuntimeModelCompat.collectRuntimeModelParts(slabState, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }

        BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
        try {
            EnhancedSlabTerrainRenderHelper.renderNested(dispatcher, slabState, blockPos, level, poseStack, consumer, parts);
        } finally {
            BlockColorRenderContext.clear();
        }
    }

    public static void submitSlabHalf(SubmitNodeCollector collector, PoseStack poseStack,
                                      Object level, net.minecraft.core.BlockPos blockPos,
                                      BlockState slabState, int packedLight) {
        List<BlockStateModelPart> parts = RuntimeModelCompat.collectRuntimeModelParts(slabState, blockPos);
        if (parts == null || parts.isEmpty()) {
            return;
        }
        var renderType = MovingBlockRenderTypeCompat.get(slabState, parts);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                LitBlockModelTesselationBridge.render(pose, consumer, level, blockPos, slabState, parts, packedLight);
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
    }
}
