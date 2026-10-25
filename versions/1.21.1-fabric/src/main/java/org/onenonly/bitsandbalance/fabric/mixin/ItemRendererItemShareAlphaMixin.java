package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multiplies item quad alpha at the final vanilla write site for old 1.21.1 GUI items.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererItemShareAlphaMixin {
	private static final Logger BITSANDBALANCE$ITEM_SHARE_LOGGER = LoggerFactory.getLogger("bitsandbalance-item-share-debug");
	private static final Set<String> BITSANDBALANCE$LOGGED_BLOCK_ALPHA_KEYS = ConcurrentHashMap.newKeySet();

	@ModifyArgs(
			method = "renderQuadList",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"
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
		bitsandbalance$logBlockQuadAlpha(originalAlpha, itemAlpha);
		args.set(5, originalAlpha * itemAlpha);
	}

	private static void bitsandbalance$logBlockQuadAlpha(float originalAlpha, float itemAlpha) {
		if (!ItemShareClient.isTrackedRenderedBlockItem()) {
			return;
		}

		String itemKey = ItemShareClient.getTrackedRenderedItemKey();
		if (itemKey.isEmpty()) {
			return;
		}

		if (!BITSANDBALANCE$LOGGED_BLOCK_ALPHA_KEYS.add(itemKey)) {
			return;
		}

		BITSANDBALANCE$ITEM_SHARE_LOGGER.info(
				"ItemShare block quad alpha: item={} originalAlpha={} itemAlpha={} finalAlpha={}",
				itemKey,
				originalAlpha,
				itemAlpha,
				originalAlpha * itemAlpha
		);
	}
}