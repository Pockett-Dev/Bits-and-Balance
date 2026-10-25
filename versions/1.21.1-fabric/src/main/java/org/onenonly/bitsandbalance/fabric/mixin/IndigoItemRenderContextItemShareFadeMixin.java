package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.ItemRenderContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces Indigo-rendered block items onto the translucent item path during old chat-share fades.
 */
@Mixin(value = ItemRenderContext.class, remap = false)
public abstract class IndigoItemRenderContextItemShareFadeMixin {
	@Shadow(remap = false)
	private ItemStack itemStack;

	@Shadow(remap = false)
	private ItemDisplayContext transformMode;

	@Shadow(remap = false)
	private boolean isDefaultTranslucent;

	@Inject(method = "computeOutputInfo", at = @At("RETURN"), require = 0, remap = false)
	private void bitsandbalance$forceTranslucentForChatItemFade(CallbackInfo ci) {
		if (!ItemShareClient.isRenderingWithAlpha() || ItemShareClient.alphaValue >= 1.0F) {
			return;
		}

		if (this.transformMode != ItemDisplayContext.GUI) {
			return;
		}

		if (this.itemStack == null || !(this.itemStack.getItem() instanceof BlockItem blockItem)) {
			return;
		}

		Block block = blockItem.getBlock();
		if (block instanceof AbstractBannerBlock || block instanceof BedBlock || block instanceof ShulkerBoxBlock) {
			return;
		}

		this.isDefaultTranslucent = true;
	}
}