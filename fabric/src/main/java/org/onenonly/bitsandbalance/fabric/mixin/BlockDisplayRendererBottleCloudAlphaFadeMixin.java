package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayFadeRenderStateAccess;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the Block Display entity renderer to detect cloud fade displays
 * and defer their rendering to after translucent terrain (see {@link CloudFadeRenderer}).
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$BlockDisplayRenderer")
public abstract class BlockDisplayRendererBottleCloudAlphaFadeMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void bitsandbalance$extractBottleCloudFadeData(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTick, CallbackInfo ci) {
        BitsAndBalanceBlockDisplayFadeRenderStateAccess access = (BitsAndBalanceBlockDisplayFadeRenderStateAccess) (Object) state;

        int argb = ((BitsAndBalanceDisplayAccess) (Object) entity).bitsandbalance$getGlowColorOverride();
        boolean isBottleCloudFade = (argb & 0x00FFFFFF) == BottleOfCloudPlacedGlass.FADE_SENTINEL_RGB;
        access.bitsandbalance$setBottleCloudFade(isBottleCloudFade);

        float alpha = 1.0F;
        if (isBottleCloudFade) {
            alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        }
        access.bitsandbalance$setBottleCloudFadeAlpha(alpha);
    }

    @Inject(
        method = "submitInner(Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bitsandbalance$submitBottleCloudAlpha(
        BlockDisplayEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        float tickDelta,
        CallbackInfo ci
    ) {
        BitsAndBalanceBlockDisplayFadeRenderStateAccess access = (BitsAndBalanceBlockDisplayFadeRenderStateAccess) (Object) state;
        if (!access.bitsandbalance$isBottleCloudFade()) return;

        float alpha = access.bitsandbalance$getBottleCloudFadeAlpha();
        if (alpha < 0.0F) alpha = 0.0F;
        if (alpha > 1.0F) alpha = 1.0F;

        int alphaByte = Math.round(alpha * 255.0F);
        if (alphaByte <= 0) {
            ci.cancel();
            return;
        }

        // Queue for deferred rendering AFTER translucent terrain so the cloud
        // composites correctly over water and other translucent blocks.
        CloudFadeRenderer.queue(poseStack.last(), alphaByte, packedLight);
        ci.cancel();
    }
}
