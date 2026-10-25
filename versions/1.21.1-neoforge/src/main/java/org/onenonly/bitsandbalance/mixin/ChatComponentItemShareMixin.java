package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.client.ChatHeadsRenderer;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public class ChatComponentItemShareMixin {
	private static final int CHAT_HEAD_ALIGNMENT_X = -1;
	private static final int CHAT_HEAD_GROUP_OFFSET_X = 1;
	private static final int CHAT_HEAD_TEXT_SPACING = 3;

	@Redirect(
			method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"
			),
			require = 1
	)
	private int bitsandbalance$drawStringWithItemShare(GuiGraphics guiGraphics, Font font, FormattedCharSequence sequence, int x, int y, int color) {
		int shiftedX = x;
		if (ClientConfig.enableChatHeads) {
			String messageText = bitsandbalance$toPlainText(sequence);
			boolean shouldRenderHead = ChatHeadsRenderer.shouldRenderChatHead(messageText);
			String playerName = shouldRenderHead ? ChatHeadsRenderer.extractPlayerName(messageText) : null;
			if (playerName != null) {
				ChatHeadsRenderer.renderChatHead(
						guiGraphics,
						playerName,
						x + ClientConfig.chatHeadOffset + CHAT_HEAD_ALIGNMENT_X + CHAT_HEAD_GROUP_OFFSET_X,
						y + ClientConfig.chatHeadOffsetY,
						ClientConfig.chatHeadSize,
						ItemShareClient.getEffectiveAlpha(guiGraphics, color)
				);
				shiftedX += ClientConfig.chatHeadSize + ClientConfig.chatHeadOffset + CHAT_HEAD_TEXT_SPACING + CHAT_HEAD_GROUP_OFFSET_X;
			}
		}

		ItemShareClient.renderItemForMessage(guiGraphics, sequence, shiftedX, y, color);
		return guiGraphics.drawString(font, sequence, shiftedX, y, color);
	}

	private static String bitsandbalance$toPlainText(FormattedCharSequence sequence) {
		StringBuilder builder = new StringBuilder();
		sequence.accept((index, style, codePoint) -> {
			builder.appendCodePoint(codePoint);
			return true;
		});
		return builder.toString();
	}
}
