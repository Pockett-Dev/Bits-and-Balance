package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.fabric.client.EnhancedSlabTerrainModelOffset;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tweaks: Enhanced Slab Behavior — Block Model Render Offset (Fabric)
 *
 * Rewrites the block-model parts used during chunk-section compilation so that
 * hanging and decoration models render at the same Y position as the already
 * adjusted interaction and support logic.
 *
 * <p>Uses {@code require = 0} because Sodium replaces the vanilla chunk
 * builder entirely and may remove the targeted methods. In that case the
 * mixin silently becomes a no-op; the outline-shape offset still applies.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public class EnhancedSlabRenderOffsetMixin {

    @Unique
    private static final ThreadLocal<ArrayDeque<Object>> bitsandbalance$restoreStates =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private static final Logger bitsandbalance$logger = LoggerFactory.getLogger("bitsandbalance-enhanced-slab-render-offset");

    @Unique
    private static final AtomicBoolean bitsandbalance$loggedHangingRender = new AtomicBoolean(false);

    @Inject(
        method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V",
        at = @At("HEAD"),
        require = 0
    )
    private void bitsandbalance$pushOffset(
            BlockAndTintGetter level,
            List<?> parts,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer buffer,
            boolean checkSides,
            int packedOverlay,
            CallbackInfo ci
    ) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || EnhancedSlabRenderOffsetContext.isInBlockEntitySubmit() || pos == null || state == null) {
            bitsandbalance$restoreStates.get().push(Boolean.FALSE);
            return;
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff == 0.0) {
            bitsandbalance$restoreStates.get().push(Boolean.FALSE);
            return;
        }

        @SuppressWarnings("unchecked")
        List<BlockModelPart> blockParts = (List<BlockModelPart>) parts;
        if (EnhancedSlabTerrainModelOffset.isWrappedPartList(blockParts)) {
            bitsandbalance$restoreStates.get().push(Boolean.FALSE);
            return;
        }
        boolean wrapped = false;

        try {
            bitsandbalance$restoreStates.get().push(EnhancedSlabTerrainModelOffset.wrapPartsInPlace(blockParts, (float) yOff));
            wrapped = true;
        } catch (UnsupportedOperationException exception) {
            poseStack.pushPose();
            poseStack.translate(0.0, yOff, 0.0);
            bitsandbalance$restoreStates.get().push(Boolean.TRUE);
        }

        bitsandbalance$logHangingRender(wrapped ? "tesselateBlock-parts" : "tesselateBlock-pose-fallback", pos, state, yOff);
    }

    @Inject(
        method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V",
        at = @At("RETURN"),
        require = 0
    )
    private void bitsandbalance$popOffset(
            BlockAndTintGetter level,
            List<?> parts,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer buffer,
            boolean checkSides,
            int packedOverlay,
            CallbackInfo ci
    ) {
        Object restoreState = bitsandbalance$restoreStates.get().pop();
        if (restoreState instanceof Object[] wrappedState) {
            EnhancedSlabTerrainModelOffset.restoreParts(wrappedState);
        } else if (Boolean.TRUE.equals(restoreState)) {
            poseStack.popPose();
        }
    }

    @Unique
    private static void bitsandbalance$logHangingRender(String path, BlockPos pos, BlockState state, double yOff) {
        if (!bitsandbalance$isTrackedHangingBlock(state)) {
            return;
        }
        if (!bitsandbalance$loggedHangingRender.compareAndSet(false, true)) {
            return;
        }

        bitsandbalance$logger.info(
                "Enhanced slab hanging render path={} block={} pos={} yOff={}",
                path,
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                pos,
                yOff
        );
    }

    @Unique
    private static boolean bitsandbalance$isTrackedHangingBlock(BlockState state) {
        return state.getBlock() instanceof LanternBlock
                || state.getBlock() instanceof BellBlock
                || state.is(net.minecraft.tags.BlockTags.CEILING_HANGING_SIGNS)
                || state.is(net.minecraft.tags.BlockTags.WALL_HANGING_SIGNS);
    }
}
