package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.fabric.client.renderer.FabricRuntimeModelCompat;
import org.onenonly.bitsandbalance.fabric.client.renderer.ReflectiveLitBlockModelTesselationBridge;

import java.util.ArrayList;
import java.util.List;

public final class CloudFadeRenderer {
    public static void queue(PoseStack.Pose currentPose, int alpha, int packedLight) {
    }

    public static void queue(PoseStack.Pose currentPose, BlockPos blockPos, int alpha, int packedLight) {
    }

    public static boolean submit(SubmitNodeCollector collector, PoseStack poseStack, BlockPos blockPos, int alpha, int packedLight) {
        Object level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        BlockState renderState = ModBottleOfCloud.cloudBlock().defaultBlockState();
        if (renderState.hasProperty(CloudBlock.TEMPORARY)) {
            renderState = renderState.setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
        }
        return submitModel(collector, poseStack, level, blockPos, renderState, alpha, packedLight);
    }

    public static boolean submitModel(SubmitNodeCollector collector, PoseStack poseStack, Object level, BlockPos blockPos, BlockState renderState, int alpha, int packedLight) {
        if (collector == null || poseStack == null || level == null || blockPos == null || renderState == null || alpha <= 0) {
            return false;
        }

        List<?> collectedParts = FabricRuntimeModelCompat.collectRuntimeModelParts(renderState, blockPos);
        if (collectedParts == null || collectedParts.isEmpty()) {
            return false;
        }

        List<BlockStateModelPart> modelParts = new ArrayList<>(collectedParts.size());
        for (Object part : collectedParts) {
            if (part instanceof BlockStateModelPart modelPart) {
                modelParts.add(modelPart);
            }
        }
        if (modelParts.isEmpty()) {
            return false;
        }

        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        RenderType renderType = RenderTypes.translucentMovingBlock();
        List<BlockStateModelPart> submittedParts = List.copyOf(modelParts);

        collector.submitCustomGeometry(poseStack, renderType, (PoseStack.Pose pose, VertexConsumer consumer) -> {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            BlockColorRenderContext.setForcedPosLong(blockPos.asLong());
            try {
                ReflectiveLitBlockModelTesselationBridge.render(
                        pose,
                        new AlphaMultiplierVertexConsumer(consumer, clampedAlpha),
                        level,
                        blockPos,
                        renderState,
                        submittedParts,
                        packedLight
                );
            } finally {
                BlockColorRenderContext.clear();
                EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            }
        });
        return true;
    }

    public static void renderAllDeferred() {
    }

    public static void renderTrackedClouds(float partialTick) {
    }

    private CloudFadeRenderer() {
    }
}
