package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Method;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.util.Mth;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies fade alpha during the actual 3D "render item into atlas" pass.
 *
 * Block-items (placeable items) render as 3D models into an item atlas framebuffer,
 * so we must ensure that render pass outputs an alpha < 1 when the chat line is fading.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererItemShareRenderToAtlasAlphaMixin {
    @Unique
    private static final ThreadLocal<boolean[]> bitsandbalance$changedShaderColorTL = ThreadLocal.withInitial(() -> new boolean[]{false});

    @Unique
    private static final ThreadLocal<float[]> bitsandbalance$previousShaderColorTL = ThreadLocal.withInitial(() -> new float[]{1.0F, 1.0F, 1.0F, 1.0F});

        @Unique
        private static volatile Method bitsandbalance$getShaderColorMethod;

        @Unique
        private static volatile Method bitsandbalance$setShaderColorMethod;

    @Inject(
            method = "renderItemToAtlas(Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;III)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$applyChatFadeAlphaToAtlasRender(
            TrackingItemStackRenderState renderState,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            int x,
            int y,
            int itemSize,
            CallbackInfo ci
    ) {
        if (!ItemShareClient.isRenderingWithAlpha()) return;

        float alpha = Mth.clamp(ItemShareClient.alphaValue, 0.0F, 1.0F);
        if (alpha >= 1.0F) return;

                try {
                        Method getMethod = bitsandbalance$getShaderColorMethod;
                        if (getMethod == null) {
                                getMethod = RenderSystem.class.getDeclaredMethod("getShaderColor");
                                getMethod.setAccessible(true);
                                bitsandbalance$getShaderColorMethod = getMethod;
                        }

                        Method setMethod = bitsandbalance$setShaderColorMethod;
                        if (setMethod == null) {
                                setMethod = RenderSystem.class.getDeclaredMethod("setShaderColor", float.class, float.class, float.class, float.class);
                                setMethod.setAccessible(true);
                                bitsandbalance$setShaderColorMethod = setMethod;
                        }

                        Object result = getMethod.invoke(null);
                        if (!(result instanceof float[] prev) || prev.length < 4) return;

                        float[] store = bitsandbalance$previousShaderColorTL.get();
                        store[0] = prev[0];
                        store[1] = prev[1];
                        store[2] = prev[2];
                        store[3] = prev[3];
                        bitsandbalance$changedShaderColorTL.get()[0] = true;

                        setMethod.invoke(null, prev[0], prev[1], prev[2], prev[3] * alpha);
                } catch (Throwable ignored) {
                        // If RenderSystem internals change, fail open (no alpha) rather than crashing.
                }
    }

    @Inject(
            method = "renderItemToAtlas(Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;III)V",
            at = @At("RETURN"),
            require = 1
    )
    private void bitsandbalance$restoreShaderColorAfterAtlasRender(
            TrackingItemStackRenderState renderState,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            int x,
            int y,
            int itemSize,
            CallbackInfo ci
    ) {
        boolean[] changed = bitsandbalance$changedShaderColorTL.get();
        if (!changed[0]) return;
        changed[0] = false;

                try {
                        Method setMethod = bitsandbalance$setShaderColorMethod;
                        if (setMethod == null) {
                                setMethod = RenderSystem.class.getDeclaredMethod("setShaderColor", float.class, float.class, float.class, float.class);
                                setMethod.setAccessible(true);
                                bitsandbalance$setShaderColorMethod = setMethod;
                        }

                        float[] prev = bitsandbalance$previousShaderColorTL.get();
                        setMethod.invoke(null, prev[0], prev[1], prev[2], prev[3]);
                } catch (Throwable ignored) {
                        // ignore
                }
    }
}
