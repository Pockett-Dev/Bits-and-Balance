package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ModelPart.class)
public abstract class ModelPartItemShareAlphaMixin {
	@Unique
	private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");

	@Unique
	private static final Set<String> BITSANDBALANCE$LOGGED_MODEL_ALPHA_KEYS = ConcurrentHashMap.newKeySet();

	@ModifyVariable(
			method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 2,
			require = 0
	)
	private int bitsandbalance$applyItemShareAlphaToSpecialItemModelPart(int color) {
		float alphaMultiplier = ItemShareClient.getTrackedSpecialBuiltInItemAlpha();
		if (alphaMultiplier >= 1.0F) {
			return color;
		}

		String itemKey = ItemShareClient.getTrackedRenderedItemKey();
		int rgb = color & 0x00FFFFFF;
		int originalAlpha = (color >>> 24) & 0xFF;
		if (originalAlpha == 0 && rgb != 0) {
			originalAlpha = 0xFF;
		}

		int scaledAlpha = Math.max(0, Math.min(255, Math.round(originalAlpha * alphaMultiplier)));
		if (!itemKey.isEmpty() && BITSANDBALANCE$LOGGED_MODEL_ALPHA_KEYS.add(itemKey)) {
			BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
					"Tracked special model alpha scale: item={} originalAlpha={} scaledAlpha={} color=0x{}",
					itemKey,
					originalAlpha,
					scaledAlpha,
					Integer.toHexString(color)
			);
		}

		return (scaledAlpha << 24) | rgb;
	}
}