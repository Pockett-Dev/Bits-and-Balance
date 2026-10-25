package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Enhanced Slab Behavior — Block Model Render Offset
 *
 * Translates the {@link PoseStack} used during chunk-section compilation so
 * that block models visually sit on the slab surface instead of floating a
 * half-block above it (bottom slabs) or below it (hanging blocks under top
 * slabs).
 *
 * <p>Uses {@code require = 0} because Sodium / Embeddium replace the vanilla
 * chunk builder entirely and may remove {@code renderBatched}.  In that case
 * the mixin silently becomes a no-op; the outline-shape offset still applies.
 */
@Mixin(BlockRenderDispatcher.class)
public class EnhancedSlabRenderOffsetMixin {

    /**
     * Per-thread flag that records whether we pushed the PoseStack at HEAD
     * so that we can pop it at RETURN.  ThreadLocal because chunk building
     * runs on worker threads.
     */
    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$didPush =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void bitsandbalance$pushOffset(
            BlockState state, BlockPos pos,
            BlockAndTintGetter level, PoseStack poseStack,
            VertexConsumer buffer, boolean checkSides,
            RandomSource random, ModelData modelData,
            RenderType renderType, CallbackInfo ci) {

        if (!Config.enableEnhancedSlabs) return;
        if (EnhancedSlabRenderOffsetContext.isInBlockEntitySubmit()) return;
        if (EnhancedSlabTerrainRenderHelper.handles(state)) return;

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff != 0.0) {
            poseStack.pushPose();
            poseStack.translate(0.0, yOff, 0.0);
            bitsandbalance$didPush.set(Boolean.TRUE);
        }
    }

    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void bitsandbalance$popOffset(
            BlockState state, BlockPos pos,
            BlockAndTintGetter level, PoseStack poseStack,
            VertexConsumer buffer, boolean checkSides,
            RandomSource random, ModelData modelData,
            RenderType renderType, CallbackInfo ci) {

        if (bitsandbalance$didPush.get()) {
            poseStack.popPose();
            bitsandbalance$didPush.set(Boolean.FALSE);
        }
    }
}
