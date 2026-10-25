package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.onenonly.bitsandbalance.fabric.client.ItemShareAlphaBufferSource;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(BlockEntityWithoutLevelRenderer.class)
public class BlockEntityWithoutLevelRendererItemStateMixin {
	private static final Logger BITSANDBALANCE$SPECIAL_ITEM_LOGGER = LoggerFactory.getLogger("bitsandbalance-special-item-debug");
	private static final Set<String> BITSANDBALANCE$LOGGED_BEWLR_KEYS = ConcurrentHashMap.newKeySet();

	private static void bitsandbalance$resetRenderState(boolean preserveItemShareAlpha) {
		float[] shaderColor = RenderSystem.getShaderColor();
		float shaderAlpha = shaderColor.length >= 4 ? shaderColor[3] : 1.0F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		float alpha = 1.0F;
		if (preserveItemShareAlpha) {
			if (ItemShareClient.isRenderingWithAlpha()) {
				alpha = Math.min(shaderAlpha, ItemShareClient.alphaValue);
			} else if (shaderAlpha < 1.0F) {
				alpha = shaderAlpha;
			}
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
	}

	private static boolean bitsandbalance$isTrackedSpecialItem(ItemStack stack) {
		if (stack.is(Items.SHIELD)) {
			return true;
		}

		if (!(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}

		return blockItem.getBlock() instanceof AbstractBannerBlock
				|| blockItem.getBlock() instanceof BedBlock
				|| blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	@ModifyVariable(method = "renderByItem", at = @At("HEAD"), argsOnly = true, require = 0)
	private MultiBufferSource bitsandbalance$wrapSpecialItemBufferForItemShareAlpha(
			MultiBufferSource original,
			ItemStack stack,
			ItemDisplayContext displayContext,
			PoseStack poseStack) {
		if (!ItemShareClient.isRenderingWithAlpha() || ItemShareClient.alphaValue >= 1.0F || !bitsandbalance$isTrackedSpecialItem(stack)) {
			return original;
		}

		return new ItemShareAlphaBufferSource(original, ItemShareClient.alphaValue);
	}

	@Inject(method = "renderByItem", at = @At("HEAD"), require = 0)
	private void bitsandbalance$resetSpecialItemShaderColorBeforeRender(
			ItemStack stack,
			ItemDisplayContext displayContext,
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			int packedOverlay,
			CallbackInfo ci) {
		bitsandbalance$resetRenderState(true);
		if (!bitsandbalance$isTrackedSpecialItem(stack)) {
			return;
		}

		String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + displayContext;
		if (!BITSANDBALANCE$LOGGED_BEWLR_KEYS.add(key)) {
			return;
		}

		BITSANDBALANCE$SPECIAL_ITEM_LOGGER.info(
				"Tracked BEWLR entry: item={} context={} light={} overlay={} shaderColor=[{},{},{},{}] itemShareActive={} itemShareAlpha={}",
				BuiltInRegistries.ITEM.getKey(stack.getItem()),
				displayContext,
				packedLight,
				packedOverlay,
				RenderSystem.getShaderColor()[0],
				RenderSystem.getShaderColor()[1],
				RenderSystem.getShaderColor()[2],
				RenderSystem.getShaderColor()[3],
				ItemShareClient.isRenderingWithAlpha(),
				ItemShareClient.alphaValue
		);
	}

	@Inject(method = "renderByItem", at = @At("RETURN"), require = 0)
	private void bitsandbalance$resetSpecialItemShaderColorAfterRender(
			ItemStack stack,
			ItemDisplayContext displayContext,
			PoseStack poseStack,
			MultiBufferSource buffer,
			int packedLight,
			int packedOverlay,
			CallbackInfo ci) {
		bitsandbalance$resetRenderState(false);
	}
}