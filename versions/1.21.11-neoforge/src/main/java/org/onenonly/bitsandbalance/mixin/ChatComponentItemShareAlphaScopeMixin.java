package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures the item-share alpha context stays valid for the entire chat render.
 *
 * In 1.21+ GUI item rendering may defer render submission until later in the
 * ChatComponent.render call. If we clear ItemShareClient's alpha state too early,
 * the submit-time mixins will see alpha=1 and the glyph won't fade.
 */
@Mixin(ChatComponent.class)
public class ChatComponentItemShareAlphaScopeMixin {

	@Inject(
			method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
			at = @At("HEAD"),
			require = 1
	)
	private void bitsandbalance$resetItemShareAlphaAtChatRenderStart(GuiGraphics guiGraphics, Font font, int tickCount, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci) {
		ItemShareClient.endAlphaRender();
	}

	@Inject(
			method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
			at = @At("RETURN"),
			require = 1
	)
	private void bitsandbalance$resetItemShareAlphaAtChatRenderEnd(GuiGraphics guiGraphics, Font font, int tickCount, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci) {
		ItemShareClient.endAlphaRender();
	}
}
