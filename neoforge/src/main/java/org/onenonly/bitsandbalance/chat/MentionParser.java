package org.onenonly.bitsandbalance.chat;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses chat messages for player mentions.
 */
public class MentionParser {
    
    public enum TriggerMode {
        AT_NAME,    // @username
        PLAIN_NAME  // username
    }
    
    /**
     * Result of parsing a message for mentions.
     */
    public static class MentionParseResult {
        private final List<MentionToken> tokens;
        private final Set<String> mentionedNames;
        
        public MentionParseResult(List<MentionToken> tokens) {
            this.tokens = tokens;
            this.mentionedNames = new HashSet<>();
            for (MentionToken token : tokens) {
                this.mentionedNames.add(token.name());
            }
        }
        
        public List<MentionToken> tokens() {
            return tokens;
        }
        
        public Set<String> mentionedNames() {
            return mentionedNames;
        }
        
        public boolean isMentioned(String playerName) {
            return mentionedNames.contains(playerName);
        }
        
        public boolean isEmpty() {
            return tokens.isEmpty();
        }
    }
    
    /**
     * Represents a mention token found in the message.
     */
    public static class MentionToken {
        private final String name;
        private final boolean isAtMention;
        private final int start;
        private final int end;
        
        public MentionToken(String name, boolean isAtMention, int start, int end) {
            this.name = name;
            this.isAtMention = isAtMention;
            this.start = start;
            this.end = end;
        }
        
        public MentionToken(String name, boolean isAtMention) {
            this(name, isAtMention, -1, -1);
        }
        
        public String name() {
            return name;
        }
        
        public boolean isAtMention() {
            return isAtMention;
        }
        
        public int start() {
            return start;
        }
        
        public int end() {
            return end;
        }
    }
    
    /**
     * Finds mentions in a chat message.
     */
    public static MentionParseResult findMentions(String raw,
            List<String> triggerModeStrings, boolean caseInsensitive, boolean wordBoundary,
            Set<String> onlineNames, int maxMentions) {
        
        List<TriggerMode> triggerModes = new ArrayList<>();
        for (String modeStr : triggerModeStrings) {
            try {
                triggerModes.add(TriggerMode.valueOf(modeStr));
            } catch (IllegalArgumentException e) {
                // Skip invalid trigger modes
            }
        }
        
        String haystack = caseInsensitive ? raw.toLowerCase(Locale.ROOT) : raw;
        List<MentionToken> tokens = new ArrayList<>();
        
        for (String name : onlineNames) {
            if (tokens.size() >= maxMentions) break;
            
            String nameToSearch = caseInsensitive ? name.toLowerCase(Locale.ROOT) : name;
            
            // Check for @name mentions
            if (triggerModes.contains(TriggerMode.AT_NAME)) {
                String atName = "@" + nameToSearch;
                if (containsToken(haystack, atName, wordBoundary)) {
                    tokens.add(new MentionToken(name, true));
                }
            }
            
            // Check for plain name mentions
            if (triggerModes.contains(TriggerMode.PLAIN_NAME)) {
                if (containsToken(haystack, nameToSearch, wordBoundary)) {
                    tokens.add(new MentionToken(name, false));
                }
            }
        }
        
        return new MentionParseResult(tokens);
    }
    
    /**
     * Checks if a token exists in the text with optional word boundary matching.
     */
    private static boolean containsToken(String text, String token, boolean wordBoundary) {
        if (wordBoundary) {
            // Use regex with word boundaries
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(token) + "\\b");
            return pattern.matcher(text).find();
        } else {
            // Simple substring search
            return text.contains(token);
        }
    }
}
