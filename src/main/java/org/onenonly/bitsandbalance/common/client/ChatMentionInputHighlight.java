package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class ChatMentionInputHighlight {
    private static final int DEFAULT_HIGHLIGHT_COLOR = 0xFF3B30;
    private static final String DISPLAY_POSITION_FIELD = "displayPos";
    private static Field displayPositionField;
    private static boolean displayPositionFieldResolved;

    private ChatMentionInputHighlight() {
    }

    public static FormattedCharSequence format(EditBox input,
                                               String visibleText,
                                               Collection<String> onlineNames,
                                               boolean allowAtName,
                                               boolean allowPlainName,
                                               boolean caseInsensitive,
                                               boolean wordBoundary,
                                               String colorHex) {
        if (input == null || visibleText == null || visibleText.isEmpty()) {
            return null;
        }
        if (!allowAtName && !allowPlainName) {
            return null;
        }

        String fullText = input.getValue();
        if (fullText == null || fullText.isEmpty() || fullText.startsWith("/")) {
            return null;
        }

        List<String> sortedNames = bitsandbalance$sortedNames(onlineNames);
        if (sortedNames.isEmpty()) {
            return null;
        }

        int visibleStart = Math.max(0, Math.min(bitsandbalance$resolveDisplayPosition(input), fullText.length()));
        int visibleEnd = Math.max(visibleStart, Math.min(visibleStart + visibleText.length(), fullText.length()));
        if (visibleStart >= visibleEnd) {
            return null;
        }

        List<MatchSpan> visibleSpans = bitsandbalance$findVisibleMatches(
                fullText,
                sortedNames,
                visibleStart,
                visibleEnd,
                allowAtName,
                allowPlainName,
                caseInsensitive,
                wordBoundary
        );
        if (visibleSpans.isEmpty()) {
            return null;
        }

        Style highlightStyle = bitsandbalance$highlightStyle(colorHex);
        List<FormattedCharSequence> parts = new ArrayList<>();
        int cursor = 0;

        for (MatchSpan span : visibleSpans) {
            int localStart = Math.max(0, span.start - visibleStart);
            int localEnd = Math.max(localStart, Math.min(span.end - visibleStart, visibleText.length()));
            if (cursor < localStart) {
                parts.add(FormattedCharSequence.forward(visibleText.substring(cursor, localStart), Style.EMPTY));
            }
            if (localStart < localEnd) {
                parts.add(FormattedCharSequence.forward(visibleText.substring(localStart, localEnd), highlightStyle));
            }
            cursor = localEnd;
        }

        if (cursor < visibleText.length()) {
            parts.add(FormattedCharSequence.forward(visibleText.substring(cursor), Style.EMPTY));
        }

        return FormattedCharSequence.composite(parts);
    }

    private static List<String> bitsandbalance$sortedNames(Collection<String> onlineNames) {
        List<String> sortedNames = new ArrayList<>();
        if (onlineNames == null) {
            return sortedNames;
        }

        for (String onlineName : onlineNames) {
            if (onlineName != null && !onlineName.isBlank()) {
                sortedNames.add(onlineName);
            }
        }

        sortedNames.sort(Comparator.comparingInt(String::length).reversed().thenComparing(String.CASE_INSENSITIVE_ORDER));
        return sortedNames;
    }

    private static List<MatchSpan> bitsandbalance$findVisibleMatches(String fullText,
                                                                     List<String> sortedNames,
                                                                     int visibleStart,
                                                                     int visibleEnd,
                                                                     boolean allowAtName,
                                                                     boolean allowPlainName,
                                                                     boolean caseInsensitive,
                                                                     boolean wordBoundary) {
        List<MatchSpan> matches = new ArrayList<>();
        int index = 0;

        while (index < fullText.length()) {
            MatchSpan fullMatch = bitsandbalance$findMatchAt(fullText, index, sortedNames, allowAtName, allowPlainName, caseInsensitive, wordBoundary);
            if (fullMatch == null) {
                index++;
                continue;
            }

            if (fullMatch.end > visibleStart && fullMatch.start < visibleEnd) {
                matches.add(new MatchSpan(Math.max(fullMatch.start, visibleStart), Math.min(fullMatch.end, visibleEnd)));
            }
            index = fullMatch.end;
        }

        return matches;
    }

    private static MatchSpan bitsandbalance$findMatchAt(String fullText,
                                                        int index,
                                                        List<String> sortedNames,
                                                        boolean allowAtName,
                                                        boolean allowPlainName,
                                                        boolean caseInsensitive,
                                                        boolean wordBoundary) {
        for (String name : sortedNames) {
            if (allowAtName && bitsandbalance$matchesAt(fullText, index, name, true, caseInsensitive, wordBoundary)) {
                return new MatchSpan(index, index + name.length() + 1);
            }
            if (allowPlainName && bitsandbalance$matchesAt(fullText, index, name, false, caseInsensitive, wordBoundary)) {
                return new MatchSpan(index, index + name.length());
            }
        }

        return null;
    }

    private static boolean bitsandbalance$matchesAt(String fullText,
                                                    int index,
                                                    String name,
                                                    boolean prefixed,
                                                    boolean caseInsensitive,
                                                    boolean wordBoundary) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        int nameStart = prefixed ? index + 1 : index;
        int tokenEnd = nameStart + name.length();
        if (tokenEnd > fullText.length()) {
            return false;
        }
        if (prefixed && (index >= fullText.length() || fullText.charAt(index) != '@')) {
            return false;
        }
        if (!fullText.regionMatches(caseInsensitive, nameStart, name, 0, name.length())) {
            return false;
        }
        if (!wordBoundary) {
            return true;
        }

        return !bitsandbalance$isWordCharacter(bitsandbalance$charAt(fullText, index - 1))
                && !bitsandbalance$isWordCharacter(bitsandbalance$charAt(fullText, tokenEnd));
    }

    private static Style bitsandbalance$highlightStyle(String colorHex) {
        TextColor color;
        try {
            String sanitized = colorHex == null ? "" : colorHex.replace("#", "");
            if (sanitized.length() > 8) {
                sanitized = sanitized.substring(0, 8);
            }
            color = TextColor.fromRgb(Integer.parseInt(sanitized, 16));
        } catch (NumberFormatException e) {
            color = TextColor.fromRgb(DEFAULT_HIGHLIGHT_COLOR);
        }
        return Style.EMPTY.withColor(color);
    }

    private static int bitsandbalance$resolveDisplayPosition(EditBox input) {
        if (!displayPositionFieldResolved) {
            displayPositionFieldResolved = true;
            try {
                displayPositionField = EditBox.class.getDeclaredField(DISPLAY_POSITION_FIELD);
                displayPositionField.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {
                displayPositionField = null;
            }
        }

        if (displayPositionField == null) {
            return 0;
        }

        try {
            return displayPositionField.getInt(input);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static char bitsandbalance$charAt(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return 0;
        }
        return text.charAt(index);
    }

    private static boolean bitsandbalance$isWordCharacter(char character) {
        return character == '_' || Character.isLetterOrDigit(character);
    }

    private static final class MatchSpan {
        private final int start;
        private final int end;

        private MatchSpan(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}