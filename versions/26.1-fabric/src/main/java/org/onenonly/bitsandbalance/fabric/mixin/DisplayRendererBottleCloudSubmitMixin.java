package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayFadeRenderStateAccess;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer")
public abstract class DisplayRendererBottleCloudSubmitMixin {
    @WrapOperation(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/DisplayRenderer;submitInner(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V"
        )
    )
    private void bitsandbalance$submitBottleCloud(
        DisplayRenderer<?, ?, ?> renderer,
        DisplayEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        float tickDelta,
        Operation<Void> original
    ) {
        if (!(state instanceof BlockDisplayEntityRenderState blockState)) {
            original.call(renderer, state, poseStack, collector, packedLight, tickDelta);
            return;
        }

        BitsAndBalanceBlockDisplayFadeRenderStateAccess access = (BitsAndBalanceBlockDisplayFadeRenderStateAccess) (Object) blockState;
        if (!access.bitsandbalance$isBottleCloudFade()) {
            original.call(renderer, state, poseStack, collector, packedLight, tickDelta);
            return;
        }

        float alpha = access.bitsandbalance$getBottleCloudFadeAlpha();
        if (alpha < 0.0F) alpha = 0.0F;
        if (alpha > 1.0F) alpha = 1.0F;

        int alphaByte = Math.round(alpha * 255.0F);
        if (alphaByte > 0) {
            CloudFadeRenderer.queue(
                poseStack.last(),
                access.bitsandbalance$getBottleCloudBlockPos(),
                alphaByte,
                packedLight
            );
        }
    }
}