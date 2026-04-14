package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a lightweight mention suggestion list above the chat box.
 */
public final class ChatMentionSuggestionOverlay {
    private static final int MAX_VISIBLE_SUGGESTIONS = 5;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int LINE_SPACING = 2;
    private static final int BOX_GAP = 2;
    private static final int INPUT_TEXT_PADDING = 4;
    private static final int BACKGROUND_COLOR = 0xC0101010;
    private static final int BORDER_COLOR = 0xD0505050;
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFF55;
    private static final int SECONDARY_TEXT_COLOR = 0xFFE0E0E0;
    private static final int OVERFLOW_TEXT_COLOR = 0xFFB0B0B0;

    private ChatMentionSuggestionOverlay() {
    }

    public static void render(GuiGraphics graphics, Font font, EditBox input, ChatMentionTabCompletion.SuggestionState suggestions) {
        if (graphics == null || font == null || input == null || suggestions == null || suggestions.matches().isEmpty()) {
            return;
        }

        List<String> displayLines = bitsandbalance$displayLines(suggestions.matches());
        if (displayLines.isEmpty()) {
            return;
        }

        int lineHeight = Math.max(font.lineHeight, 9);
        int contentWidth = 0;
        for (String line : displayLines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }

        int innerLeft = input.getX() + INPUT_TEXT_PADDING;
        int innerRight = input.getX() + input.getWidth() - INPUT_TEXT_PADDING;
        int maxBoxWidth = Math.max(32, innerRight - innerLeft);
        int boxWidth = Math.min(maxBoxWidth, contentWidth + (PADDING_X * 2));
        if (boxWidth <= 0) {
            return;
        }

        int boxHeight = (PADDING_Y * 2) + (displayLines.size() * lineHeight) + ((displayLines.size() - 1) * LINE_SPACING);
        int boxX = bitsandbalance$resolveBoxX(font, input, suggestions, innerLeft, innerRight, boxWidth);
        int boxY = input.getY() - boxHeight - BOX_GAP;
        if (boxY < 2) {
            boxY = input.getY() + input.getHeight() + BOX_GAP;
        }

        bitsandbalance$fill(graphics, boxX, boxY, boxX + boxWidth, boxY + boxHeight, BACKGROUND_COLOR);
        bitsandbalance$fill(graphics, boxX, boxY, boxX + boxWidth, boxY + 1, BORDER_COLOR);
        bitsandbalance$fill(graphics, boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, BORDER_COLOR);

        for (int index = 0; index < displayLines.size(); index++) {
            String line = displayLines.get(index);
            int textY = boxY + PADDING_Y + (index * (lineHeight + LINE_SPACING));
            int color = index == 0 ? PRIMARY_TEXT_COLOR : SECONDARY_TEXT_COLOR;
            if (index == displayLines.size() - 1 && suggestions.matches().size() > MAX_VISIBLE_SUGGESTIONS) {
                color = OVERFLOW_TEXT_COLOR;
            }
            graphics.drawString(font, line, boxX + PADDING_X, textY, color, true);
        }
    }

    private static List<String> bitsandbalance$displayLines(List<String> matches) {
        List<String> lines = new ArrayList<>();
        int visibleCount = Math.min(matches.size(), MAX_VISIBLE_SUGGESTIONS);
        for (int index = 0; index < visibleCount; index++) {
            lines.add(matches.get(index));
        }
        int remaining = matches.size() - visibleCount;
        if (remaining > 0) {
            lines.add("... +" + remaining + " more");
        }
        return lines;
    }

    private static int bitsandbalance$resolveBoxX(Font font,
                                                  EditBox input,
                                                  ChatMentionTabCompletion.SuggestionState suggestions,
                                                  int innerLeft,
                                                  int innerRight,
                                                  int boxWidth) {
        String value = input.getValue();
        if (value == null) {
            return innerLeft;
        }

        int visibleStart = Math.max(0, Math.min(bitsandbalance$resolveDisplayPos(input), value.length()));
        int tokenStart = Math.max(0, Math.min(suggestions.startInclusive(), value.length()));
        int tokenEnd = Math.max(tokenStart, Math.min(suggestions.endExclusive(), value.length()));

        String visiblePrefix = value.substring(Math.min(visibleStart, tokenStart), tokenStart);
        String tokenText = value.substring(tokenStart, tokenEnd);
        String usernamePortion = tokenText.length() > 1 ? tokenText.substring(1) : "";

        int tokenLeft = innerLeft + font.width(visiblePrefix);
        int usernameStartX = tokenLeft + font.width("@");
        int usernameWidth = Math.max(font.width(usernamePortion), font.width("_"));
        int anchoredX = usernameStartX - PADDING_X;

        if (usernameWidth > boxWidth) {
            anchoredX -= (usernameWidth - boxWidth) / 2;
        }

        int minX = innerLeft;
        int maxX = Math.max(innerLeft, innerRight - boxWidth);
        if (anchoredX < minX) {
            return minX;
        }
        if (anchoredX > maxX) {
            return maxX;
        }
        return anchoredX;
    }

    private static int bitsandbalance$resolveDisplayPos(EditBox input) {
        Integer displayPos = bitsandbalance$readIntField(input, "displayPos");
        if (displayPos != null) {
            return displayPos;
        }

        displayPos = bitsandbalance$readIntField(input, "displayPosition");
        if (displayPos != null) {
            return displayPos;
        }

        return 0;
    }

    private static Integer bitsandbalance$readIntField(EditBox input, String fieldName) {
        try {
            var field = input.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(input);
            if (value instanceof Integer integer) {
                return integer;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static void bitsandbalance$fill(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, bottom, color);
    }
}