package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.onenonly.bitsandbalance.fabric.client.FabricChatHeadsRenderer;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentHoverMixin {
    private static final int CHAT_HEAD_GROUP_OFFSET_X = 1;
    private static final int CHAT_HEAD_TEXT_SPACING = 3;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    private double screenToChatX(double screenX) {
        throw new AssertionError();
    }

    @Shadow
    private double screenToChatY(double screenY) {
        throw new AssertionError();
    }

    @Shadow
    private int getMessageLineIndexAt(double chatX, double chatY) {
        throw new AssertionError();
    }

    @Inject(
            method = "getClickedComponentStyleAt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bitsandbalance$alignHoverWithRenderedChat(
            double mouseX,
            double mouseY,
            CallbackInfoReturnable<Style> callback
    ) {
        if (!FabricClientConfig.enableChatHeads) {
            return;
        }

        double chatX = screenToChatX(mouseX);
        double chatY = screenToChatY(mouseY);
        int lineIndex = getMessageLineIndexAt(chatX, chatY);
        if (lineIndex < 0 || lineIndex >= trimmedMessages.size()) {
            callback.setReturnValue(null);
            return;
        }

        FormattedCharSequence sequence = trimmedMessages.get(lineIndex).content();
        String messageText = bitsandbalance$hoverPlainText(sequence);
        if (!FabricChatHeadsRenderer.shouldRenderChatHead(messageText)) {
            return;
        }

        String playerName = FabricChatHeadsRenderer.resolvePlayerName(messageText);
        int nameStart = bitsandbalance$findHoverPlayerNameStart(messageText, playerName);
        if (nameStart < 0) {
            return;
        }

        int splitIndex = messageText.codePointCount(0, nameStart);
        int prefixWidth = minecraft.font.width(bitsandbalance$hoverSlice(sequence, 0, splitIndex));
        int contentShift = FabricClientConfig.chatHeadSize
                + FabricClientConfig.chatHeadOffset
                + CHAT_HEAD_TEXT_SPACING
                + CHAT_HEAD_GROUP_OFFSET_X;

        // The timestamp prefix stays at its original x position. Only the
        // sender and message move right to make room for the rendered head.
        if (chatX >= prefixWidth && chatX < prefixWidth + contentShift) {
            callback.setReturnValue(null);
            return;
        }

        double contentX = chatX >= prefixWidth + contentShift
                ? chatX - contentShift
                : chatX;
        callback.setReturnValue(minecraft.font.getSplitter().componentStyleAtWidth(
                sequence,
                Mth.floor(contentX)
        ));
    }

    private static int bitsandbalance$findHoverPlayerNameStart(String messageText, String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return -1;
        }

        int nameStart = messageText.indexOf(playerName);
        while (nameStart >= 0) {
            int nameEnd = nameStart + playerName.length();
            boolean startsAtBoundary = nameStart == 0
                    || !bitsandbalance$isHoverPlayerNameCharacter(messageText.charAt(nameStart - 1));
            boolean endsAtBoundary = nameEnd == messageText.length()
                    || !bitsandbalance$isHoverPlayerNameCharacter(messageText.charAt(nameEnd));
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

    private static boolean bitsandbalance$isHoverPlayerNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static FormattedCharSequence bitsandbalance$hoverSlice(
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

    private static String bitsandbalance$hoverPlainText(FormattedCharSequence sequence) {
        StringBuilder builder = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }
}
