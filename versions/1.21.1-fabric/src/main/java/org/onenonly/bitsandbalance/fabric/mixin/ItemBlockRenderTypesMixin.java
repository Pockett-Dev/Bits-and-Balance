package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forces a translucent item sheet so inline chat items can fade smoothly.
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {
	private static final Logger BITSANDBALANCE$ITEM_SHARE_LOGGER = LoggerFactory.getLogger("bitsandbalance-item-share-debug");
	private static final Set<String> BITSANDBALANCE$LOGGED_RENDER_TYPE_KEYS = ConcurrentHashMap.newKeySet();

	@Inject(
			method = "getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/RenderType;",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private static void bitsandbalance$inheritEnhancedSlabSourceRenderLayer(BlockState state, CallbackInfoReturnable<RenderType> cir) {
		// Terrain rendering swaps generated slab-family geometry to source-slab quads, so the chunk layer has to match the source.
		BlockState sourceState = bitsandbalance$enhancedSlabSourceRenderState(state);
		if (sourceState == null) {
			return;
		}

		cir.setReturnValue(ItemBlockRenderTypes.getChunkRenderType(sourceState));
	}

	@Inject(
			method = "getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private static void bitsandbalance$overrideItemRenderType(ItemStack stack, boolean renderCutout, CallbackInfoReturnable<RenderType> cir) {
		if (!bitsandbalance$shouldOverrideRenderType()) {
			return;
		}

		if (stack.getItem() instanceof BlockItem blockItem && bitsandbalance$isSpecialBuiltInBlock(blockItem.getBlock())) {
			return;
		}

		bitsandbalance$logRenderTypeOverride(stack, renderCutout, Sheets.translucentItemSheet());
		cir.setReturnValue(Sheets.translucentItemSheet());
	}

	@Inject(
			method = "getRenderType(Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/client/renderer/RenderType;",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private static void bitsandbalance$overrideRenderType(BlockState state, boolean renderCutout, CallbackInfoReturnable<RenderType> cir) {
		if (state == null) {
			return;
		}

		if (bitsandbalance$shouldOverrideRenderType()) {
			if (bitsandbalance$isSpecialBuiltInBlock(state.getBlock())) {
				return;
			}

			cir.setReturnValue(Sheets.translucentItemSheet());
			return;
		}

		BlockState sourceState = bitsandbalance$enhancedSlabSourceRenderState(state);
		if (sourceState != null) {
			cir.setReturnValue(ItemBlockRenderTypes.getRenderType(sourceState, renderCutout));
		}
	}

	private static boolean bitsandbalance$shouldOverrideRenderType() {
		return ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F;
	}

	private static boolean bitsandbalance$isSpecialBuiltInBlock(Block block) {
		return block instanceof AbstractBannerBlock || block instanceof BedBlock || block instanceof ShulkerBoxBlock;
	}

	private static BlockState bitsandbalance$enhancedSlabSourceRenderState(BlockState state) {
		if (state == null) {
			return null;
		}

		Block sourceSlab = EnhancedSlabItemSupport.sourceSlabForBlock(state.getBlock());
		if (sourceSlab == null || sourceSlab == state.getBlock()) {
			return null;
		}

		BlockState normalizedSource = DynamicVariantMaterialHelper.normalizeSlabState(sourceSlab.defaultBlockState());
		return normalizedSource != null ? normalizedSource : sourceSlab.defaultBlockState();
	}

	private static void bitsandbalance$logRenderTypeOverride(ItemStack stack, boolean renderCutout, RenderType renderType) {
		String itemKey = ItemShareClient.getTrackedRenderedItemKey();
		if (itemKey.isEmpty()) {
			return;
		}

		String logKey = itemKey + "|" + renderCutout + "|" + renderType;
		if (!BITSANDBALANCE$LOGGED_RENDER_TYPE_KEYS.add(logKey)) {
			return;
		}

		BITSANDBALANCE$ITEM_SHARE_LOGGER.info(
				"ItemShare block render type override: item={} renderCutout={} alpha={} renderType={}",
				itemKey,
				renderCutout,
				ItemShareClient.alphaValue,
				renderType
		);
	}
}
