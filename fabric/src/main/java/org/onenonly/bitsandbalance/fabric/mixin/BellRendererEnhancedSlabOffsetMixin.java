package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BellRenderer.class)
public class BellRendererEnhancedSlabOffsetMixin {

    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$pushed = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$offsetBellBody(BlockEntityRenderState renderState,
                                               PoseStack poseStack,
                                               SubmitNodeCollector collector,
                                               CameraRenderState cameraRenderState,
                                               CallbackInfo ci) {
        if (!(renderState instanceof BellRenderState bellState)) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        if (!FabricTweaksConfig.enableEnhancedSlabs || bellState == null || bellState.blockPos == null) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        BlockState blockState = ((BlockEntityRenderStateAccessor) bellState).bitsandbalance$getBlockState();
        if (blockState == null) {
            blockState = level.getBlockState(bellState.blockPos);
        }
        if (!(blockState.getBlock() instanceof BellBlock)) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, bellState.blockPos, blockState);
        if (yOff == 0.0D) {
            bitsandbalance$pushed.set(Boolean.FALSE);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, yOff, 0.0D);
        bitsandbalance$pushed.set(Boolean.TRUE);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("RETURN"), require = 0)
    private void bitsandbalance$restoreBellBody(BlockEntityRenderState renderState,
                                                PoseStack poseStack,
                                                SubmitNodeCollector collector,
                                                CameraRenderState cameraRenderState,
                                                CallbackInfo ci) {
        if (bitsandbalance$pushed.get()) {
            poseStack.popPose();
            bitsandbalance$pushed.set(Boolean.FALSE);
        }
    }
}