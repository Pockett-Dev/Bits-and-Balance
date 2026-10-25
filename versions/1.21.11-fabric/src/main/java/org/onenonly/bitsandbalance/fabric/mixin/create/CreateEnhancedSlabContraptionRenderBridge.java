package org.onenonly.bitsandbalance.fabric.mixin.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.BlockColorRenderContext;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;

import java.lang.reflect.Method;
import java.util.List;

final class CreateEnhancedSlabContraptionRenderBridge {
    private CreateEnhancedSlabContraptionRenderBridge() {
    }

    static boolean renderFlywheel(Object emitters,
                                  BlockRenderDispatcher dispatcher,
                                  ModelBlockRenderer blockRenderer,
                                  BlockAndTintGetter level,
                                  PoseStack poseStack,
                                  BlockPos pos,
                                  BlockState ownerState,
                                  boolean aoEnabled) {
        return renderGroups(dispatcher, blockRenderer, level, poseStack, pos, ownerState,
                (renderState, parts) -> {
                    VertexConsumer consumer = flywheelEmitter(emitters, ItemBlockRenderTypes.getChunkRenderType(renderState), aoEnabled);
                    if (consumer == null) {
                        return false;
                    }
                    renderGroup(blockRenderer, level, poseStack, pos, renderState, parts, consumer);
                    return true;
                });
    }

    static boolean renderUniversal(Object bufferSource,
                                   Object universalEmitter,
                                   BlockRenderDispatcher dispatcher,
                                   ModelBlockRenderer blockRenderer,
                                   BlockAndTintGetter level,
                                   PoseStack poseStack,
                                   BlockPos pos,
                                   BlockState ownerState) {
        return renderGroups(dispatcher, blockRenderer, level, poseStack, pos, ownerState,
                (renderState, parts) -> {
                    VertexConsumer consumer = universalEmitter(bufferSource, universalEmitter, ItemBlockRenderTypes.getChunkRenderType(renderState));
                    if (consumer == null) {
                        return false;
                    }
                    renderGroup(blockRenderer, level, poseStack, pos, renderState, parts, consumer);
                    return true;
                });
    }

    private static boolean renderGroups(BlockRenderDispatcher dispatcher,
                                        ModelBlockRenderer blockRenderer,
                                        BlockAndTintGetter level,
                                        PoseStack poseStack,
                                        BlockPos pos,
                                        BlockState ownerState,
                                        GroupRenderer renderer) {
        if (dispatcher == null || blockRenderer == null || level == null || poseStack == null || pos == null || ownerState == null) {
            return false;
        }

        boolean rendered = false;
        List<EnhancedSlabTerrainRenderHelper.RenderGroup> groups;
        if (EnhancedSlabTerrainRenderHelper.rendersWithoutBlockEntity(ownerState)) {
            groups = EnhancedSlabTerrainRenderHelper.getOrBuildFixedRenderPlan(ownerState);
        } else if (EnhancedSlabTerrainRenderHelper.handles(ownerState)) {
            groups = EnhancedSlabTerrainRenderHelper.getOrBuildCreateRenderPlan(level, pos, ownerState);
        } else {
            return false;
        }

        for (EnhancedSlabTerrainRenderHelper.RenderGroup group : groups) {
            if (group == null || group.renderState() == null || group.parts() == null || group.parts().isEmpty()) {
                continue;
            }
            rendered |= renderer.render(group.renderState(), group.parts());
        }
        return rendered;
    }

    private static void renderGroup(ModelBlockRenderer blockRenderer,
                                    BlockAndTintGetter level,
                                    PoseStack poseStack,
                                    BlockPos pos,
                                    BlockState renderState,
                                    List<BlockModelPart> parts,
                                    VertexConsumer consumer) {
        poseStack.pushPose();
        BlockColorRenderContext.setForcedPosLong(pos.asLong());
        try {
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            blockRenderer.tesselateBlock(level, parts, renderState, pos, poseStack, consumer, true, OverlayTexture.NO_OVERLAY);
        } finally {
            BlockColorRenderContext.clear();
            poseStack.popPose();
        }
    }

    private static VertexConsumer flywheelEmitter(Object emitters, ChunkSectionLayer renderType, boolean aoEnabled) {
        if (emitters == null || renderType == null) {
            return null;
        }
        try {
            Method getEmitter = emitters.getClass().getDeclaredMethod("getEmitter", ChunkSectionLayer.class);
            getEmitter.setAccessible(true);
            Object emitter = getEmitter.invoke(emitters, renderType);
            if (!(emitter instanceof VertexConsumer consumer)) {
                return null;
            }

            Method prepare = emitter.getClass().getMethod("prepareForModelLayer", boolean.class);
            prepare.invoke(emitter, aoEnabled);
            return consumer;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static VertexConsumer universalEmitter(Object bufferSource, Object universalEmitter, ChunkSectionLayer renderType) {
        if (bufferSource == null || universalEmitter == null || renderType == null) {
            return null;
        }
        try {
            Method prepare = universalEmitter.getClass().getMethod("prepare", bufferSource.getClass().getInterfaces()[0], ChunkSectionLayer.class);
            prepare.invoke(universalEmitter, bufferSource, renderType);
            return universalEmitter instanceof VertexConsumer consumer ? consumer : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface GroupRenderer {
        boolean render(BlockState renderState, List<BlockModelPart> parts);
    }
}
