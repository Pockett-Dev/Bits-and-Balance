package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.onenonly.bitsandbalance.client.renderer.LitBlockModelTesselationBridge;
import org.onenonly.bitsandbalance.client.renderer.RuntimeModelCompat;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;

import java.util.ArrayList;
import java.util.List;

/**
 * Deferred renderer for the Bottle-of-Cloud display entity's alpha fade.
 * Display entities render before translucent terrain, so bottle-cloud visuals
 * are queued and drawn after translucent blocks with the resource-pack model.
 */
public final class CloudFadeRenderer {
    private static final List<QueuedCloud> QUEUE = new ArrayList<>();

    private record QueuedCloud(Matrix4f pose, Matrix3f normal, BlockPos blockPos, int alpha, int packedLight) {}

    public static void queue(PoseStack.Pose currentPose, BlockPos blockPos, int alpha, int packedLight) {
        if (currentPose == null || alpha <= 0) {
            return;
        }

        QUEUE.add(new QueuedCloud(
                new Matrix4f(currentPose.pose()),
                new Matrix3f(currentPose.normal()),
                blockPos != null ? blockPos.immutable() : BlockPos.ZERO,
                alpha,
                packedLight
        ));
    }

    public static void renderAllDeferred() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || QUEUE.isEmpty()) {
            return;
        }

        try {
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            RenderType renderType = RenderTypes.translucentMovingBlock();
            VertexConsumer baseConsumer = bufferSource.getBuffer(renderType);
            BlockState renderState = temporaryCloudRenderState();
            PoseStack poseStack = new PoseStack();

            for (QueuedCloud cloud : QUEUE) {
                List<BlockStateModelPart> parts = RuntimeModelCompat.collectRuntimeModelParts(renderState, cloud.blockPos);
                if (parts == null || parts.isEmpty()) {
                    continue;
                }

                poseStack.pushPose();
                poseStack.last().pose().set(cloud.pose);
                poseStack.last().normal().set(cloud.normal);
                LitBlockModelTesselationBridge.render(
                        poseStack.last(),
                        new AlphaMultiplierVertexConsumer(baseConsumer, cloud.alpha),
                        level,
                        cloud.blockPos,
                        renderState,
                        parts,
                        cloud.packedLight
                );
                poseStack.popPose();
            }

            bufferSource.endBatch(renderType);
        } finally {
            QUEUE.clear();
        }
    }

    private static BlockState temporaryCloudRenderState() {
        return ModBottleOfCloud.cloudBlock().defaultBlockState().setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
    }

    private CloudFadeRenderer() {
    }
}
