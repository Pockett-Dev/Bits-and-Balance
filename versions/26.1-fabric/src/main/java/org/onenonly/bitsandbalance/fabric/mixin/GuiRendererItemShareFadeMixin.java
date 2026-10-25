package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.onenonly.bitsandbalance.fabric.client.ItemShareGuiItemAlpha;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies per-item chat fade alpha to GUI items at the final blit-from-atlas stage.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererItemShareFadeMixin {
    @Unique
    private static final ThreadLocal<Float> bitsandbalance$itemShareAlphaTL = ThreadLocal.withInitial(() -> 1.0F);

    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$hasItemShareAlphaTL = ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$captureItemShareAlpha(
            net.minecraft.client.renderer.state.gui.GuiItemRenderState renderState,
            net.minecraft.client.gui.render.GuiItemAtlas.SlotView slotView,
            CallbackInfo ci
    ) {
        if ((Object) renderState instanceof ItemShareGuiItemAlpha alphaCarrier && alphaCarrier.bitsandbalance$hasItemShareAlpha()) {
            float alpha = alphaCarrier.bitsandbalance$getItemShareAlpha();
            bitsandbalance$itemShareAlphaTL.set(alpha);
            bitsandbalance$hasItemShareAlphaTL.set(true);
        } else {
            bitsandbalance$itemShareAlphaTL.set(1.0F);
            bitsandbalance$hasItemShareAlphaTL.set(false);
        }
    }

    @Inject(
            method = "submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V",
            at = @At("RETURN"),
            require = 1
    )
    private void bitsandbalance$clearItemShareAlpha(
            net.minecraft.client.renderer.state.gui.GuiItemRenderState renderState,
            net.minecraft.client.gui.render.GuiItemAtlas.SlotView slotView,
            CallbackInfo ci
    ) {
        bitsandbalance$itemShareAlphaTL.set(1.0F);
        bitsandbalance$hasItemShareAlphaTL.set(false);
    }

    @ModifyArg(
            method = "submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
            ),
            index = 11,
            require = 1
    )
    private int bitsandbalance$applyItemShareAlphaToItemBlitColor(int originalColor) {
        if (!bitsandbalance$hasItemShareAlphaTL.get()) return originalColor;
        float alpha = bitsandbalance$itemShareAlphaTL.get();
        if (alpha >= 1.0F) return originalColor;
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        // Premultiplied tint for GUI_TEXTURED_PREMULTIPLIED_ALPHA.
        int out = ARGB.color(a, a, a, a);
        return out;
    }
}
