package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.onenonly.bitsandbalance.fabric.client.FabricChatHeadsRenderer;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Renders inline shared-item icons on the older 1.21.1 chat draw path.
 */
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
	private int bitsandbalance$drawStringWithItemShare(
			GuiGraphics guiGraphics,
			Font font,
			FormattedCharSequence sequence,
			int x,
			int y,
			int color
	) {
		if (FabricClientConfig.enableChatHeads) {
			String messageText = bitsandbalance$toPlainText(sequence);
			boolean shouldRenderHead = FabricChatHeadsRenderer.shouldRenderChatHead(messageText);
			String playerName = shouldRenderHead ? FabricChatHeadsRenderer.resolvePlayerName(messageText) : null;
			if (playerName != null) {
				int nameStart = bitsandbalance$findPlayerNameStart(messageText, playerName);
				if (nameStart >= 0) {
					int splitIndex = messageText.codePointCount(0, nameStart);
					FormattedCharSequence prefix = bitsandbalance$slice(sequence, 0, splitIndex);
					FormattedCharSequence suffix = bitsandbalance$slice(sequence, splitIndex, Integer.MAX_VALUE);
					int prefixWidth = font.width(prefix);
					int contentShift = FabricClientConfig.chatHeadSize
							+ FabricClientConfig.chatHeadOffset
							+ CHAT_HEAD_TEXT_SPACING
							+ CHAT_HEAD_GROUP_OFFSET_X;

					guiGraphics.drawString(font, prefix, x, y, color);
					FabricChatHeadsRenderer.renderChatHead(
							guiGraphics,
							playerName,
							x + prefixWidth + FabricClientConfig.chatHeadOffset + CHAT_HEAD_ALIGNMENT_X + CHAT_HEAD_GROUP_OFFSET_X,
							y + FabricClientConfig.chatHeadOffsetY,
							FabricClientConfig.chatHeadSize,
							ItemShareClient.getEffectiveAlpha(guiGraphics, color)
					);
					ItemShareClient.renderItemForMessage(guiGraphics, sequence, (float) (x + contentShift), (float) y, color);
					return guiGraphics.drawString(font, suffix, x + prefixWidth + contentShift, y, color);
				}
			}
		}

		ItemShareClient.renderItemForMessage(guiGraphics, sequence, (float) x, (float) y, color);
		return guiGraphics.drawString(font, sequence, x, y, color);
	}

	private static int bitsandbalance$findPlayerNameStart(String messageText, String playerName) {
		int nameStart = messageText.indexOf(playerName);
		while (nameStart >= 0) {
			int nameEnd = nameStart + playerName.length();
			boolean startsAtBoundary = nameStart == 0 || !bitsandbalance$isPlayerNameCharacter(messageText.charAt(nameStart - 1));
			boolean endsAtBoundary = nameEnd == messageText.length() || !bitsandbalance$isPlayerNameCharacter(messageText.charAt(nameEnd));
			if (startsAtBoundary && endsAtBoundary) {
				int openingBracket = messageText.lastIndexOf('<', nameStart);
				int closingBracket = messageText.indexOf('>', nameEnd);
				if (openingBracket >= 0 && closingBracket >= nameEnd) {
					return openingBracket;
				}
				return nameStart;
			}
			nameStart = messageText.indexOf(playerName, nameStart + 1);
		}
		return -1;
	}

	private static boolean bitsandbalance$isPlayerNameCharacter(char character) {
		return Character.isLetterOrDigit(character) || character == '_';
	}

	private static FormattedCharSequence bitsandbalance$slice(
			FormattedCharSequence sequence,
			int start,
			int end
	) {
		return sink -> {
			int[] position = {0};
			return sequence.accept((ignored, style, codePoint) -> {
				int current = position[0]++;
				if (current < start) {
					return true;
				}
				if (current >= end) {
					return false;
				}
				return sink.accept(current - start, style, codePoint);
			});
		};
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
