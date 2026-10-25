package org.onenonly.bitsandbalance.common.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Resolves Tab completion for @mentions in chat input.
 */
public final class ChatMentionTabCompletion {
    private ChatMentionTabCompletion() {
    }

    public static CompletionResult complete(String input, int cursor, Collection<String> onlineNames) {
        SuggestionState suggestions = suggestions(input, cursor, onlineNames);
        if (suggestions == null) {
            return null;
        }
        return complete(input, suggestions);
    }

    public static SuggestionState suggestions(String input, int cursor, Collection<String> onlineNames) {
        if (input == null || input.isEmpty() || onlineNames == null || onlineNames.isEmpty()) {
            return null;
        }
        if (cursor < 0 || cursor > input.length()) {
            return null;
        }

        MentionToken token = findToken(input, cursor);
        if (token == null) {
            return null;
        }

        List<String> matches = findMatches(token.partialName(), onlineNames);
        if (matches.isEmpty()) {
            return null;
        }

        return new SuggestionState(token.startInclusive(), token.endExclusive(), token.partialName(), List.copyOf(matches));
    }

    public static CompletionResult complete(String input, SuggestionState suggestions) {
        if (input == null || suggestions == null || suggestions.matches().isEmpty()) {
            return null;
        }

        String matchedName = suggestions.matches().getFirst();
        int replaceEnd = suggestions.endExclusive();
        String replacement = "@" + matchedName;
        boolean addTrailingSpace = replaceEnd == input.length();
        if (addTrailingSpace) {
            replacement += " ";
        }

        String updated = input.substring(0, suggestions.startInclusive()) + replacement + input.substring(replaceEnd);
        int updatedCursor = suggestions.startInclusive() + replacement.length();
        return new CompletionResult(updated, updatedCursor, matchedName);
    }

    private static MentionToken findToken(String input, int cursor) {
        if (cursor == 0) {
            return null;
        }

        int tokenStart = cursor - 1;
        while (tokenStart >= 0 && !Character.isWhitespace(input.charAt(tokenStart))) {
            tokenStart--;
        }
        tokenStart++;

        if (tokenStart >= cursor || input.charAt(tokenStart) != '@') {
            return null;
        }

        for (int index = tokenStart + 1; index < cursor; index++) {
            if (!isUsernameCharacter(input.charAt(index))) {
                return null;
            }
        }

        int tokenEnd = cursor;
        while (tokenEnd < input.length() && isUsernameCharacter(input.charAt(tokenEnd))) {
            tokenEnd++;
        }

        String partialName = input.substring(tokenStart + 1, cursor);
        return new MentionToken(tokenStart, tokenEnd, partialName);
    }

    private static List<String> findMatches(String partialName, Collection<String> onlineNames) {
        String normalized = partialName.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String onlineName : onlineNames) {
            if (onlineName == null || onlineName.isBlank()) {
                continue;
            }
            if (!normalized.isEmpty() && !onlineName.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                continue;
            }
            matches.add(onlineName);
        }

        matches.sort(
                Comparator.comparingInt(String::length)
                        .thenComparing(name -> name.toLowerCase(Locale.ROOT))
                        .thenComparing(Comparator.naturalOrder())
        );
        return matches;
    }

    private static boolean isUsernameCharacter(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '_';
    }

    public record CompletionResult(String updatedText, int cursorPosition, String matchedName) {
    }

    public record SuggestionState(int startInclusive, int endExclusive, String partialName, List<String> matches) {
    }

    private record MentionToken(int startInclusive, int endExclusive, String partialName) {
    }
}