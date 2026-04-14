package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayFadeRenderStateAccess;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
import org.onenonly.bitsandbalance.common.client.CloudFadeRenderer;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudPlacedGlass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts the Block Display entity renderer to detect cloud fade displays
 * and defer their rendering to after translucent terrain (see {@link CloudFadeRenderer}).
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.DisplayRenderer$BlockDisplayRenderer")
public abstract class BlockDisplayRendererBottleCloudAlphaFadeMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-bottle-cloud-fade");
    private static final Map<Long, String> LAST_DEBUG_SIGNATURE = new ConcurrentHashMap<>();

    private static void bitsandbalance$extractFadeData(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTick) {
        BitsAndBalanceBlockDisplayFadeRenderStateAccess access = (BitsAndBalanceBlockDisplayFadeRenderStateAccess) (Object) state;

        BlockPos blockPos = BlockPos.containing(entity.position());
        int argb = ((BitsAndBalanceDisplayAccess) (Object) entity).bitsandbalance$getGlowColorOverride();
        boolean isBottleCloudFade = (argb & 0x00FFFFFF) == BottleOfCloudPlacedGlass.FADE_SENTINEL_RGB;
        access.bitsandbalance$setBottleCloudFade(isBottleCloudFade);

        float alpha = 1.0F;
        if (isBottleCloudFade) {
            TemporaryCloudBlockEntity temporaryCloud = entity.level().getBlockEntity(blockPos) instanceof TemporaryCloudBlockEntity blockEntity
                ? blockEntity
                : null;
            boolean hasBlockEntity = temporaryCloud != null;
            boolean hasActiveFade = hasBlockEntity && temporaryCloud.hasActiveFade();
            int glowAlpha = (argb >>> 24) & 0xFF;
            int alphaByte;
            String source;
            if (hasActiveFade) {
                alphaByte = temporaryCloud.getRenderAlpha(partialTick);
                source = "block_entity";
            } else {
                alphaByte = glowAlpha;
                source = "glow";
            }
            alpha = alphaByte / 255.0F;

            if (SharedConstants.IS_RUNNING_IN_IDE) {
                String signature = source
                    + '|'
                    + (alphaByte / 16)
                    + '|'
                    + hasBlockEntity
                    + '|'
                    + hasActiveFade
                    + '|'
                    + glowAlpha;
                String previous = LAST_DEBUG_SIGNATURE.put(blockPos.asLong(), signature);
                if (!signature.equals(previous)) {
                    LOGGER.info(
                        "Bottle o' Cloud fade client probe pos={} source={} alpha={} glowAlpha={} bePresent={} beActive={} partialTick={}",
                        blockPos,
                        source,
                        alphaByte,
                        glowAlpha,
                        hasBlockEntity,
                        hasActiveFade,
                        partialTick
                    );
                }
            }
        } else if (SharedConstants.IS_RUNNING_IN_IDE) {
            LAST_DEBUG_SIGNATURE.remove(blockPos.asLong());
        }
        access.bitsandbalance$setBottleCloudFadeAlpha(alpha);
        access.bitsandbalance$setBottleCloudBlockPos(blockPos);
    }

    private static boolean bitsandbalance$submitFade(BlockDisplayEntityRenderState state, PoseStack poseStack, int packedLight) {
        BitsAndBalanceBlockDisplayFadeRenderStateAccess access = (BitsAndBalanceBlockDisplayFadeRenderStateAccess) (Object) state;
        if (!access.bitsandbalance$isBottleCloudFade()) {
            return false;
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

        return true;
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void bitsandbalance$extractBottleCloudFadeData(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTick, CallbackInfo ci) {
        bitsandbalance$extractFadeData(entity, state, partialTick);
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Display;Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void bitsandbalance$extractBottleCloudFadeDataBridge(Display entity, DisplayEntityRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Display.BlockDisplay blockDisplay && state instanceof BlockDisplayEntityRenderState blockState) {
            bitsandbalance$extractFadeData(blockDisplay, blockState, partialTick);
        }
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
        if (bitsandbalance$submitFade(state, poseStack, packedLight)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "submitInner(Lnet/minecraft/client/renderer/entity/state/DisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bitsandbalance$submitBottleCloudAlphaBridge(
        DisplayEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int packedLight,
        float tickDelta,
        CallbackInfo ci
    ) {
        if (state instanceof BlockDisplayEntityRenderState blockState && bitsandbalance$submitFade(blockState, poseStack, packedLight)) {
            ci.cancel();
        }
    }
}
