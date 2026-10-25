package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if mc == 1.21.11
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
//? endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
//? if mc != 26.2 && mc != 26.3
import net.minecraft.client.renderer.MultiBufferSource;
//? endif
//? if mc == 1.21.1
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
//? else
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
//? endif
//? endif
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
//? if mc == 1.21.11
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
//? endif
import org.joml.Matrix3f;
import org.joml.Matrix4f;
//? if mc == 1.21.11
import net.minecraft.world.phys.Vec3;
//? endif

import java.util.ArrayList;
import java.util.List;

/**
 * Deferred renderer for the Bottle-of-Cloud display entity's alpha fade.
 * <p>
 * Display entities render BEFORE translucent terrain (water). If the cloud
 * renders during the entity pass it either:
 * <ul>
 *   <li>Blocks water (depth write on) — water vanishes behind cloud</li>
 *   <li>Gets overwritten by water (depth write off) — cloud invisible where water is</li>
 * </ul>
 * <p>
 * By deferring the draw to AFTER translucent terrain (via a level render event),
 * the cloud renders on top of water with proper alpha blending.
 */
public final class CloudFadeRenderer {
    //? if mc == 1.21.11
    private static final int FULL_BRIGHT = 0x00F000F0;
    //? endif

    /** Queued cloud renders for the current frame. Filled during entity pass, drawn after translucent terrain. */
    private static final List<QueuedCloud> QUEUE = new ArrayList<>();

    /** Snapshot of one cloud's rendering data. */
    private record QueuedCloud(Matrix4f pose, Matrix3f normal, BlockPos blockPos, int alpha, int packedLight) {}

    /**
     * Queue a cloud for deferred rendering. Called from the cloud fade mixin
     * during the entity rendering pass (instead of drawing immediately).
     */
    public static void queue(PoseStack.Pose currentPose, int alpha, int packedLight) {
        queue(currentPose, BlockPos.ZERO, alpha, packedLight);
    }

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

    /**
     * Flush all queued clouds. Call this from a level render event that fires
     * AFTER translucent terrain (NeoForge: AfterTranslucentBlocks, Fabric: END_MAIN).
     */
    public static void renderAllDeferred() {
        if (QUEUE.isEmpty()) return;

//? if mc == 26.2 || mc == 26.3
        QUEUE.clear();
//? else
        try {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) {
                return;
            }

            MultiBufferSource.BufferSource bufferSource =
                    minecraft.renderBuffers().bufferSource();
            RenderType renderType = CloudRenderType.get();
            VertexConsumer baseConsumer = bufferSource.getBuffer(renderType);
            BlockState renderState = temporaryCloudRenderState();

            PoseStack tempStack = new PoseStack();

            for (QueuedCloud cloud : QUEUE) {
                tempStack.pushPose();
                tempStack.last().pose().set(cloud.pose);
                tempStack.last().normal().set(cloud.normal);

                renderCloudModel(
                        minecraft,
                        level,
                        renderState,
                        cloud.blockPos,
                        tempStack,
                        new AlphaMultiplierVertexConsumer(baseConsumer, cloud.alpha),
                        cloud.packedLight
                );

                tempStack.popPose();
            }

            // Flush the cloud geometry to the framebuffer immediately
            bufferSource.endBatch(renderType);
        } finally {
            QUEUE.clear();
        }
//? endif
    }

    //? if mc == 1.21.11
    public static void renderTrackedClouds(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        Vec3 cameraPos = cameraPosition(minecraft);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        RenderType renderType = CloudRenderType.get();
        VertexConsumer baseConsumer = bufferSource.getBuffer(renderType);
        PoseStack poseStack = new PoseStack();

        boolean rendered = false;
        for (BlockPos pos : TemporaryCloudBlockEntity.getTrackedPositions(level)) {
            if (!(level.getBlockEntity(pos) instanceof TemporaryCloudBlockEntity blockEntity)) {
                continue;
            }

            int alpha = blockEntity.getRenderAlpha(partialTick);
            if (alpha <= 0) {
                continue;
            }

            BlockState blockState = blockEntity.getBlockState();
            if (blockState == null || !blockState.hasProperty(CloudBlock.TEMPORARY) || !blockState.getValue(CloudBlock.TEMPORARY)) {
                continue;
            }
            BlockState renderState = temporaryCloudRenderState();

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            try {
                renderCloudModel(
                    minecraft,
                    level,
                    renderState,
                    pos,
                    poseStack,
                    new AlphaMultiplierVertexConsumer(baseConsumer, alpha),
                    samplePackedLight(level, renderState, pos)
                );
            } finally {
                poseStack.popPose();
            }
            rendered = true;
        }

        if (rendered) {
            bufferSource.endBatch(renderType);
        }
    }
    //? endif

    private static BlockState temporaryCloudRenderState() {
        return ModBottleOfCloud.cloudBlock().defaultBlockState();
    }

    private static void renderCloudModel(Minecraft minecraft, ClientLevel level, BlockState renderState, BlockPos blockPos,
                                         PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        //? if mc == 1.21.1
        BakedModel model = dispatcher.getBlockModel(renderState);
        dispatcher.getModelRenderer().tesselateBlock(
                level,
                model,
                renderState,
                blockPos,
                poseStack,
                consumer,
                false,
                RandomSource.create(),
                renderState.getSeed(blockPos),
                OverlayTexture.NO_OVERLAY
        );
        //? else
        //? if mc != 26.1 && mc != 26.2 && mc != 26.3
        BlockStateModel model = dispatcher.getBlockModel(renderState);
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(renderState.getSeed(blockPos)));
        if (parts == null || parts.isEmpty()) {
            return;
        }
        var renderLevel = CloudModelLightBridge.wrapForModelShading(level);
        boolean externalLightApplied = CloudModelLightBridge.setExternalLight(level, packedLight);
        try {
            dispatcher.renderBatched(renderState, blockPos, renderLevel, poseStack, consumer, false, parts);
        } finally {
            if (externalLightApplied) {
                CloudModelLightBridge.resetExternalLight(level);
            }
        }
        //? endif
        //? endif
    }

    //? if mc == 1.21.11
    private static Vec3 cameraPosition(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer != null ? minecraft.gameRenderer.getMainCamera() : null;
        if (camera != null) {
            return camera.position();
        }
        return minecraft.getCameraEntity() != null ? minecraft.getCameraEntity().getEyePosition() : Vec3.ZERO;
    }

    private static int samplePackedLight(ClientLevel level, BlockState state, BlockPos pos) {
        try {
            int light = LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, level, state, pos);
            return light != 0 ? light : FULL_BRIGHT;
        } catch (RuntimeException ignored) {
            return FULL_BRIGHT;
        }
    }
    //? endif

    private CloudFadeRenderer() {}
}
