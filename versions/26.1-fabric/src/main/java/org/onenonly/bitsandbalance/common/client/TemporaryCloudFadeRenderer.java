package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.fabric.client.renderer.FabricRuntimeModelCompat;
import org.onenonly.bitsandbalance.fabric.client.renderer.VirtualRenderWorldLightBridge;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class TemporaryCloudFadeRenderer {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final List<QueuedCloud> QUEUE = new ArrayList<>();

    private record QueuedCloud(Matrix4f pose, Matrix3f normal, BlockPos blockPos, int alpha, int packedLight) {}

    public static void queue(PoseStack.Pose currentPose, BlockPos blockPos, int alpha, int packedLight) {
        if (currentPose == null || blockPos == null || alpha <= 0) {
            return;
        }

        QUEUE.add(new QueuedCloud(
                new Matrix4f(currentPose.pose()),
                new Matrix3f(currentPose.normal()),
                blockPos.immutable(),
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
            VertexConsumer consumer = bufferSource.getBuffer(renderType);
            BlockState renderState = temporaryCloudRenderState();
            PoseStack poseStack = new PoseStack();

            for (QueuedCloud cloud : QUEUE) {
                List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(renderState, cloud.blockPos);
                if (parts == null || parts.isEmpty()) {
                    continue;
                }

                poseStack.pushPose();
                poseStack.last().pose().set(cloud.pose);
                poseStack.last().normal().set(cloud.normal);
                try {
                    VirtualRenderWorldLightBridge.renderBatched(
                            renderState,
                            cloud.blockPos,
                            level,
                            poseStack,
                            new AlphaMultiplierVertexConsumer(consumer, cloud.alpha),
                            true,
                            parts,
                            cloud.packedLight
                    );
                } finally {
                    poseStack.popPose();
                }
            }

            bufferSource.endBatch(renderType);
        } finally {
            QUEUE.clear();
        }
    }

    public static void renderAll() {
        renderAllDeferred();
    }

    public static void renderTrackedClouds(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        Vec3 cameraPos = cameraPosition(minecraft);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderTypes.translucentMovingBlock();
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
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

            BlockState renderState = blockEntity.getBlockState();
            if (!isTemporaryCloud(renderState)) {
                continue;
            }
            renderState = renderState.setValue(CloudBlock.TEMPORARY, Boolean.TRUE);

            List<?> parts = FabricRuntimeModelCompat.collectRuntimeModelParts(renderState, pos);
            if (parts == null || parts.isEmpty()) {
                continue;
            }
            int packedLight = samplePackedLight(level, renderState, pos);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            try {
                VirtualRenderWorldLightBridge.renderBatched(
                        renderState,
                        pos,
                        level,
                        poseStack,
                        new AlphaMultiplierVertexConsumer(consumer, alpha),
                        true,
                        parts,
                        packedLight
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

    private static BlockState temporaryCloudRenderState() {
        return ModBottleOfCloud.cloudBlock().defaultBlockState().setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
    }

    private static boolean isTemporaryCloud(BlockState state) {
        return state != null && state.hasProperty(CloudBlock.TEMPORARY) && state.getValue(CloudBlock.TEMPORARY);
    }

    private static Vec3 cameraPosition(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer != null ? minecraft.gameRenderer.getMainCamera() : null;
        if (camera != null) {
            Vec3 pos = cameraPosition(camera);
            if (pos != null) {
                return pos;
            }
        }
        return minecraft.getCameraEntity() != null ? minecraft.getCameraEntity().getEyePosition() : Vec3.ZERO;
    }

    private static Vec3 cameraPosition(Camera camera) {
        for (String methodName : new String[]{"getPosition", "getPos", "position"}) {
            try {
                Method method = camera.getClass().getMethod(methodName);
                Object result = method.invoke(camera);
                if (result instanceof Vec3 pos) {
                    return pos;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static int samplePackedLight(ClientLevel level, BlockState state, BlockPos pos) {
        try {
            Class<?> levelRendererClass = Class.forName("net.minecraft.client.renderer.LevelRenderer");
            Class<?> brightnessGetterClass = null;
            for (Class<?> nested : levelRendererClass.getDeclaredClasses()) {
                if (nested.getSimpleName().equals("BrightnessGetter")) {
                    brightnessGetterClass = nested;
                    break;
                }
            }
            if (brightnessGetterClass == null) {
                return FULL_BRIGHT;
            }

            Object brightnessGetter = brightnessGetterClass.getField("DEFAULT").get(null);
            Method target = null;
            for (Method candidate : levelRendererClass.getMethods()) {
                if (!candidate.getName().equals("getLightColor") || candidate.getParameterCount() != 4) {
                    continue;
                }
                Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (!parameterTypes[0].isAssignableFrom(brightnessGetterClass) && !brightnessGetterClass.isAssignableFrom(parameterTypes[0])) {
                    continue;
                }
                if (!BlockState.class.isAssignableFrom(parameterTypes[2])) {
                    continue;
                }
                if (!BlockPos.class.isAssignableFrom(parameterTypes[3])) {
                    continue;
                }
                target = candidate;
                break;
            }
            if (target == null) {
                return FULL_BRIGHT;
            }

            Object result = target.invoke(null, brightnessGetter, level, state, pos);
            return result instanceof Integer light && light != 0 ? light : FULL_BRIGHT;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return FULL_BRIGHT;
        }
    }

    private TemporaryCloudFadeRenderer() {
    }
}
