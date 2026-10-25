package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
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
import java.util.function.Function;

@Mixin(Material.class)
public abstract class MaterialItemShareRenderTypeMixin {
	@Unique
	private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");

	@Unique
	private static final ResourceLocation BITSANDBALANCE$SHULKER_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/shulker_boxes.png");

	@Unique
	private static final ResourceLocation BITSANDBALANCE$BED_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/beds.png");

	@Unique
	private static final ResourceLocation BITSANDBALANCE$BANNER_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/banner_patterns.png");

	@Unique
	private static final ResourceLocation BITSANDBALANCE$SHIELD_SHEET_ATLAS = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/shield_patterns.png");

	@Unique
	private static final Set<String> BITSANDBALANCE$LOGGED_DYNAMIC_RENDER_TYPES = ConcurrentHashMap.newKeySet();

	@Inject(method = "renderType", at = @At("HEAD"), cancellable = true, require = 0)
	private void bitsandbalance$useDynamicSpecialItemRenderType(
			Function<ResourceLocation, RenderType> renderTypeGetter,
			CallbackInfoReturnable<RenderType> cir) {
		float alphaMultiplier = ItemShareClient.getTrackedSpecialBuiltInItemAlpha();
		if (alphaMultiplier >= 1.0F) {
			return;
		}

		Material material = (Material) (Object) this;
		ResourceLocation atlas = material.atlasLocation();
		if (!BITSANDBALANCE$usesSpecialBuiltInAtlas(atlas)) {
			return;
		}

		String itemKey = ItemShareClient.getTrackedRenderedItemKey();
		if (itemKey.isEmpty()) {
			return;
		}

		RenderType originalRenderType = renderTypeGetter.apply(atlas);
		RenderType replacementRenderType = bitsandbalance$getSpecialItemRenderTypeOverride(itemKey, atlas, originalRenderType);
		if (replacementRenderType == null || replacementRenderType == originalRenderType) {
			return;
		}

		String key = itemKey + "|" + atlas + "|" + originalRenderType;
		if (BITSANDBALANCE$LOGGED_DYNAMIC_RENDER_TYPES.add(key)) {
			BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
					"Tracked special material renderType override: item={} atlas={} alpha={} original={} replacement={}",
					itemKey,
					atlas,
					alphaMultiplier,
					originalRenderType,
					replacementRenderType
			);
		}

		cir.setReturnValue(replacementRenderType);
	}

	@Unique
	private static RenderType bitsandbalance$getSpecialItemRenderTypeOverride(
			String itemKey,
			ResourceLocation atlas,
			RenderType originalRenderType) {
		if (itemKey.endsWith("_banner")) {
			return RenderType.itemEntityTranslucentCull(atlas);
		}

		if ("minecraft:shield".equals(itemKey)) {
			if (originalRenderType == RenderType.entitySolid(atlas)) {
				return RenderType.itemEntityTranslucentCull(atlas);
			}

			if (originalRenderType == RenderType.entityNoOutline(atlas)) {
				return RenderType.itemEntityTranslucentCull(atlas);
			}

			if (originalRenderType == RenderType.entityCutout(atlas)) {
				return RenderType.itemEntityTranslucentCull(atlas);
			}
		}

		return RenderType.entityTranslucent(atlas);
	}

	@Unique
	private static boolean BITSANDBALANCE$usesSpecialBuiltInAtlas(ResourceLocation atlas) {
		return BITSANDBALANCE$SHULKER_SHEET_ATLAS.equals(atlas)
				|| BITSANDBALANCE$BED_SHEET_ATLAS.equals(atlas)
				|| BITSANDBALANCE$BANNER_SHEET_ATLAS.equals(atlas)
				|| BITSANDBALANCE$SHIELD_SHEET_ATLAS.equals(atlas);
	}
}