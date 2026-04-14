package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public class ChatComponentItemShareMixin {
	@Redirect(
			method = "lambda$render$1",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"
			)
	)
	private void bitsandbalance$drawStringWithItemShare(GuiGraphics guiGraphics, Font font, FormattedCharSequence sequence, int x, int y, int color) {
		ItemShareClient.renderItemForMessage(guiGraphics, sequence, x, y, color);
		guiGraphics.drawString(font, sequence, x, y, color);
	}
}
