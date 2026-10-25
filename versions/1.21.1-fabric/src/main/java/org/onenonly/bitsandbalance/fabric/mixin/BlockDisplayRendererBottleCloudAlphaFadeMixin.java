package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
public abstract class BlockDisplayRendererBottleCloudAlphaFadeMixin {

    @Inject(
        method = "renderInner(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/world/entity/Display$BlockDisplay$BlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void bitsandbalance$renderBottleCloudAlpha(
        Display.BlockDisplay entity,
        Display.BlockDisplay.BlockRenderState renderState,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        float partialTick,
        CallbackInfo ci
    ) {
        int argb = ((BitsAndBalanceDisplayAccess) (Object) entity).bitsandbalance$getGlowColorOverride();
        boolean isBottleCloudFade = (argb & 0x00FFFFFF) == BottleOfCloudPlacedGlass.FADE_SENTINEL_RGB;
        if (!isBottleCloudFade) {
            return;
        }

        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        int alphaByte = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        if (alphaByte <= 0) {
            ci.cancel();
            return;
        }

        CloudFadeRenderer.queue(poseStack.last(), BlockPos.containing(entity.position()), alphaByte, packedLight);
        ci.cancel();
    }
}
