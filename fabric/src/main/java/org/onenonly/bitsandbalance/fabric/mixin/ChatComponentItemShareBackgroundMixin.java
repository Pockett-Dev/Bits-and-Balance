package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the chat line draw path so inline item icons fade with chat text.
 *
 * Split by target type to avoid @Shadow mapping conflicts.
 */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public class ChatComponentItemShareBackgroundMixin {
	@Shadow
	@Final
	private GuiGraphics graphics;

	@Inject(method = "handleMessage", at = @At("HEAD"), require = 0)
	private void bitsandbalance$beginChatLineAlpha(int y, float opacity, FormattedCharSequence sequence, CallbackInfoReturnable<Boolean> cir) {
		ItemShareClient.beginChatLineAlpha(opacity);
		ItemShareClient.renderItemForMessage(this.graphics, sequence, 0.0F, (float) y, 0xFFFFFFFF);
	}

	@Inject(method = "handleMessage", at = @At("RETURN"), require = 0)
	private void bitsandbalance$endChatLineAlpha(int y, float opacity, FormattedCharSequence sequence, CallbackInfoReturnable<Boolean> cir) {
		ItemShareClient.endChatLineAlpha();
	}
}
