package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Model.class)
public abstract class ModelItemShareRenderTypeMixin {
	@Unique
	private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");

	@Unique
	private static final Set<String> BITSANDBALANCE$LOGGED_MODEL_RENDER_TYPES = ConcurrentHashMap.newKeySet();

	@Inject(method = "renderType", at = @At("RETURN"), cancellable = true, require = 0)
	private void bitsandbalance$overrideShieldModelRenderType(ResourceLocation texture, CallbackInfoReturnable<RenderType> cir) {
		float alphaMultiplier = ItemShareClient.getTrackedSpecialBuiltInItemAlpha();
		if (alphaMultiplier >= 1.0F) {
			return;
		}

		String itemKey = ItemShareClient.getTrackedRenderedItemKey();
		if (!"minecraft:shield".equals(itemKey)) {
			return;
		}

		RenderType originalRenderType = cir.getReturnValue();
		RenderType replacementRenderType = originalRenderType == RenderType.armorCutoutNoCull(texture)
				? RenderType.itemEntityTranslucentCull(texture)
				: RenderType.itemEntityTranslucentCull(texture);
		String key = itemKey + "|" + texture + "|" + originalRenderType;
		if (BITSANDBALANCE$LOGGED_MODEL_RENDER_TYPES.add(key)) {
			BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
					"Tracked special model renderType override: item={} texture={} alpha={} original={} replacement={}",
					itemKey,
					texture,
					alphaMultiplier,
					originalRenderType,
					replacementRenderType
			);
		}

		cir.setReturnValue(replacementRenderType);
	}
}