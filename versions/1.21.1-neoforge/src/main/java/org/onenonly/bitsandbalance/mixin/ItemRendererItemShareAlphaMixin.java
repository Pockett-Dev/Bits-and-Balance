package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiplies item quad alpha at the final vanilla write site for old 1.21.1 GUI items.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererItemShareAlphaMixin {
	@Redirect(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/BakedModel;getRenderTypes(Lnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;"
			),
			require = 0
	)
	private List<RenderType> bitsandbalance$useTranslucentRenderTypesDuringItemShareFade(BakedModel model, ItemStack itemStack, boolean fabulous) {
		List<RenderType> renderTypes = model.getRenderTypes(itemStack, fabulous);
		if (!ItemShareClient.isRenderingWithAlpha() || ItemShareClient.alphaValue >= 1.0F) {
			return renderTypes;
		}

		List<RenderType> fadedRenderTypes = null;
		for (int index = 0; index < renderTypes.size(); index++) {
			RenderType renderType = renderTypes.get(index);
			RenderType fadedRenderType = bitsandbalance$fadedRenderType(renderType);
			if (fadedRenderType != renderType && fadedRenderTypes == null) {
				fadedRenderTypes = new ArrayList<>(renderTypes);
			}
			if (fadedRenderTypes != null) {
				fadedRenderTypes.set(index, fadedRenderType);
			}
		}

		return fadedRenderTypes == null ? renderTypes : fadedRenderTypes;
	}

	private RenderType bitsandbalance$fadedRenderType(RenderType renderType) {
		if (renderType == Sheets.cutoutBlockSheet() || renderType == Sheets.solidBlockSheet()) {
			return Sheets.translucentCullBlockSheet();
		}
		return renderType;
	}

	@ModifyArgs(
			method = "renderQuadList",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
			),
			require = 0
	)
	private void bitsandbalance$applyItemShareAlphaToQuadWrite(Args args) {
		if (!ItemShareClient.isRenderingWithAlpha()) {
			return;
		}

		float itemAlpha = ItemShareClient.alphaValue;
		if (itemAlpha >= 1.0F) {
			return;
		}

		float originalAlpha = args.get(5);
		args.set(5, originalAlpha * itemAlpha);
	}
}