package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.onenonly.bitsandbalance.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies per-item chat fade alpha to GUI items at the final blit-from-atlas stage.
 *
 * GuiRenderer renders all items into an offscreen atlas, then blits them to the screen using
 * GUI_TEXTURED_PREMULTIPLIED_ALPHA with color=-1. That ignores chat fade.
 *
 * We tag the GuiItemRenderState at submission time (see GuiGraphicsItemShareMixin) and then
 * modulate the blit color here so the item fades in sync with chat text.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererItemShareFadeMixin {
	@Unique
	private static final ThreadLocal<Float> bitsandbalance$itemShareAlphaTL = ThreadLocal.withInitial(() -> 1.0F);
	@Unique
	private static final ThreadLocal<Boolean> bitsandbalance$hasItemShareAlphaTL = ThreadLocal.withInitial(() -> false);

	@Inject(
			method = "submitBlitFromItemAtlas(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;FFII)V",
			at = @At("HEAD"),
			require = 1
	)
	private void bitsandbalance$captureItemShareAlpha(
			net.minecraft.client.gui.render.state.GuiItemRenderState renderState,
			float u,
			float v,
			int itemSize,
			int atlasSize,
			CallbackInfo ci
	) {
		if ((Object) renderState instanceof ItemShareGuiItemAlpha alphaCarrier && alphaCarrier.bitsandbalance$hasItemShareAlpha()) {
			bitsandbalance$itemShareAlphaTL.set(alphaCarrier.bitsandbalance$getItemShareAlpha());
			bitsandbalance$hasItemShareAlphaTL.set(true);
		} else {
			bitsandbalance$itemShareAlphaTL.set(1.0F);
			bitsandbalance$hasItemShareAlphaTL.set(false);
		}
	}

	@Inject(
			method = "submitBlitFromItemAtlas(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;FFII)V",
			at = @At("RETURN"),
			require = 1
	)
	private void bitsandbalance$clearItemShareAlpha(
			net.minecraft.client.gui.render.state.GuiItemRenderState renderState,
			float u,
			float v,
			int itemSize,
			int atlasSize,
			CallbackInfo ci
	) {
		bitsandbalance$itemShareAlphaTL.set(1.0F);
		bitsandbalance$hasItemShareAlphaTL.set(false);
	}

	@ModifyArg(
			method = "submitBlitFromItemAtlas(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;FFII)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/render/state/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
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
		return ARGB.color(a, a, a, a);
	}
}
