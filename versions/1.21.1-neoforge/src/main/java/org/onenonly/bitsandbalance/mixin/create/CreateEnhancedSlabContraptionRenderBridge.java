package org.onenonly.bitsandbalance.mixin.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;

import java.lang.reflect.Method;
import java.util.Iterator;

final class CreateEnhancedSlabContraptionRenderBridge {
    private CreateEnhancedSlabContraptionRenderBridge() {
    }

    static boolean renderFlywheelBlockFallback(Object emitters,
                                               BlockRenderDispatcher dispatcher,
                                               ModelBlockRenderer blockRenderer,
                                               BlockAndTintGetter level,
                                               PoseStack poseStack,
                                               BlockPos pos,
                                               BlockState ownerState,
                                               boolean aoEnabled) {
        if (!canRender(dispatcher, blockRenderer, level, poseStack, pos, ownerState)) {
            return false;
        }

        VertexConsumer consumer = flywheelEmitter(emitters, firstRenderType(dispatcher, level, pos), aoEnabled);
        if (consumer == null) {
            return false;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            return EnhancedSlabTerrainRenderHelper.renderCreateContraption(
                    dispatcher,
                    level,
                    pos,
                    ownerState,
                    poseStack,
                    consumer,
                    OverlayTexture.NO_OVERLAY
            );
        } finally {
            poseStack.popPose();
        }
    }

    static boolean renderFlywheelModelFallback(Object emitters,
                                               BlockRenderDispatcher dispatcher,
                                               ModelBlockRenderer blockRenderer,
                                               BlockAndTintGetter level,
                                               PoseStack poseStack,
                                               BlockPos pos,
                                               BlockState ownerState,
                                               boolean aoEnabled) {
        if (!canRender(dispatcher, blockRenderer, level, poseStack, pos, ownerState)) {
            return false;
        }

        VertexConsumer consumer = flywheelEmitter(emitters, firstRenderType(dispatcher, level, pos), aoEnabled);
        if (consumer == null) {
            return false;
        }

        poseStack.pushPose();
        try {
            return EnhancedSlabTerrainRenderHelper.renderCreateContraption(
                    dispatcher,
                    level,
                    pos,
                    ownerState,
                    poseStack,
                    consumer,
                    OverlayTexture.NO_OVERLAY
            );
        } finally {
            poseStack.popPose();
        }
    }

    private static boolean canRender(BlockRenderDispatcher dispatcher,
                                     ModelBlockRenderer blockRenderer,
                                     BlockAndTintGetter level,
                                     PoseStack poseStack,
                                     BlockPos pos,
                                     BlockState ownerState) {
        return dispatcher != null
                && blockRenderer != null
                && level != null
                && poseStack != null
                && pos != null
                && ownerState != null
                && EnhancedSlabTerrainRenderHelper.handles(ownerState);
    }

    private static RenderType firstRenderType(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos) {
        Object slots = EnhancedSlabTerrainRenderHelper.materialSource(level, pos);
        if (slots != null) {
            for (int index = 0; index < 4; index++) {
                BlockState slot = EnhancedSlabTerrainRenderHelper.materialSlot(slots, index);
                RenderType renderType = firstRenderType(dispatcher, slot);
                if (renderType != null) {
                    return renderType;
                }
            }
        }
        return RenderType.solid();
    }

    private static @Nullable RenderType firstRenderType(BlockRenderDispatcher dispatcher, @Nullable BlockState state) {
        if (dispatcher == null || state == null) {
            return null;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        if (model == null) {
            return null;
        }
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY);
        if (renderTypes == null || renderTypes.isEmpty()) {
            return null;
        }

        Iterator<RenderType> iterator = renderTypes.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static @Nullable VertexConsumer flywheelEmitter(Object emitters, RenderType renderType, boolean aoEnabled) {
        if (emitters == null || renderType == null) {
            return null;
        }
        try {
            Method getEmitter = emitters.getClass().getDeclaredMethod("getEmitter", RenderType.class);
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
}
