package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(targets = "net.minecraft.client.renderer.blockentity.AbstractSignRenderer")
public class AbstractSignRendererEnhancedSlabOffsetMixin {

    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$pushed = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Unique
    private static final Logger bitsandbalance$logger = LoggerFactory.getLogger("bitsandbalance-enhanced-slab-render-offset");

    @Unique
    private static final AtomicBoolean bitsandbalance$logged = new AtomicBoolean(false);

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$pushSignOffset(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || state == null || state.blockPos == null) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        BlockState blockState = ((BlockEntityRenderStateAccessor) state).bitsandbalance$getBlockState();
        if (blockState == null
                || (!blockState.is(BlockTags.CEILING_HANGING_SIGNS) && !blockState.is(BlockTags.WALL_HANGING_SIGNS))) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, state.blockPos, blockState);
        if (yOff == 0.0D) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, yOff, 0.0D);
        bitsandbalance$pushed.set(Boolean.TRUE);

        if (bitsandbalance$logged.compareAndSet(false, true)) {
            bitsandbalance$logger.info(
                    "Enhanced slab hanging sign renderer path block={} pos={} yOff={}",
                    BuiltInRegistries.BLOCK.getKey(blockState.getBlock()),
                    state.blockPos,
                    yOff
            );
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"), require = 0)
    private void bitsandbalance$popSignOffset(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (bitsandbalance$pushed.get()) {
            poseStack.popPose();
            bitsandbalance$pushed.set(Boolean.FALSE);
        }
    }
}