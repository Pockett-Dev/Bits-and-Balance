package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweaks: Enhanced Slab Behavior — Block Entity Render Offset (Fabric)
 *
 * Translates the {@link PoseStack} used by {@link BlockEntityRenderDispatcher}
 * so that block-entity models (signs, banners, bells, skulls, etc.) visually
 * sit on the slab surface instead of floating a half-block above it.
 *
 * <p>The caller ({@code LevelRenderer.submitBlockEntities}) already pushes and
 * pops the pose stack around each entity, so we only need to add an extra
 * {@code translate(0, offset, 0)} — no push/pop of our own is required.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class EnhancedSlabBlockEntityRenderOffsetMixin {

    private static final ThreadLocal<Boolean> bitsandbalance$pushed =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "submit", at = @At("HEAD"))
    private <S extends BlockEntityRenderState> void bitsandbalance$offsetBlockEntity(
            S state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState, CallbackInfo ci) {

        if (!FabricTweaksConfig.enableEnhancedSlabs) return;

        BlockPos pos = state.blockPos;
        BlockState blockState = state.blockState;
        if (pos == null || blockState == null) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, blockState);
        if (yOff != 0.0) {
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            bitsandbalance$pushed.set(Boolean.TRUE);
            poseStack.translate(0.0, yOff, 0.0);
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private <S extends BlockEntityRenderState> void bitsandbalance$endSubmit(
            S state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState, CallbackInfo ci) {
        if (bitsandbalance$pushed.get()) {
            EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            bitsandbalance$pushed.set(Boolean.FALSE);
        }
    }
}
