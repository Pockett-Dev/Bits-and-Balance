package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.onenonly.bitsandbalance.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tags GUI item render submissions with the current chat fade alpha.
 *
 * In 1.21+, GuiGraphics.renderItem(...) does not render immediately; it submits a GuiItemRenderState
 * which is later rendered into an item atlas by GuiRenderer. We must carry alpha per submitted item.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsItemShareMixin {

	@ModifyArg(
			method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"
			),
			index = 0,
			require = 1
	)
	private GuiItemRenderState bitsandbalance$tagItemShareAlpha(GuiItemRenderState renderState) {
		if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
			((ItemShareGuiItemAlpha)(Object) renderState).bitsandbalance$setItemShareAlpha(ItemShareClient.alphaValue);
		}
		return renderState;
	}
}
