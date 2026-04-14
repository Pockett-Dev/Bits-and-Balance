package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Tweaks: Enhanced Slab Behavior — Block Model Render Offset (Fabric)
 *
 * Translates the {@link PoseStack} used during chunk-section compilation so
 * that block models visually sit on the slab surface instead of floating a
 * half-block above it (bottom slabs) or below it (hanging blocks under top
 * slabs).
 *
 * <p>Uses {@code require = 0} because Sodium replaces the vanilla chunk
 * builder entirely and may remove {@code renderBatched}.  In that case the
 * mixin silently becomes a no-op; the outline-shape offset still applies.
 */
@Mixin(BlockRenderDispatcher.class)
public class EnhancedSlabRenderOffsetMixin {

    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$didPush =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(
        method = "renderBatched",
        at = @At("HEAD"),
        require = 0
    )
    private void bitsandbalance$pushOffset(
            BlockState state, BlockPos pos,
            BlockAndTintGetter level, PoseStack poseStack,
            VertexConsumer buffer, boolean checkSides,
            List<?> renderTypes, CallbackInfo ci) {

        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (EnhancedSlabRenderOffsetContext.isInBlockEntitySubmit()) return;

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff != 0.0) {
            poseStack.pushPose();
            poseStack.translate(0.0, yOff, 0.0);
            bitsandbalance$didPush.set(Boolean.TRUE);
        }
    }

    @Inject(
        method = "renderBatched",
        at = @At("RETURN"),
        require = 0
    )
    private void bitsandbalance$popOffset(
            BlockState state, BlockPos pos,
            BlockAndTintGetter level, PoseStack poseStack,
            VertexConsumer buffer, boolean checkSides,
            List<?> renderTypes, CallbackInfo ci) {

        if (bitsandbalance$didPush.get()) {
            poseStack.popPose();
            bitsandbalance$didPush.set(Boolean.FALSE);
        }
    }
}
