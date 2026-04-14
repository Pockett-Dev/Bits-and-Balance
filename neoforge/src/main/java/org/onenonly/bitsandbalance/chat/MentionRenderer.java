package org.onenonly.bitsandbalance.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.onenonly.bitsandbalance.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders chat messages with highlighted mentions.
 */
public class MentionRenderer {
    
    /**
     * Renders a message for a specific recipient, highlighting only their mentions.
     */
    public static Component renderForRecipient(String raw, MentionParser.MentionParseResult parsed, String recipientName) {
        if (parsed.isEmpty()) {
            return Component.literal(raw);
        }
        
        // Only highlight if this recipient is mentioned
        if (parsed.isMentioned(recipientName)) {
            return highlightMentions(raw, parsed, true);
        } else {
            return Component.literal(raw);
        }
    }
    
    /**
     * Renders a message for all viewers, highlighting all mentions.
     */
    public static Component renderForAll(String raw, MentionParser.MentionParseResult parsed) {
        if (parsed.isEmpty()) {
            return Component.literal(raw);
        }
        
        return highlightMentions(raw, parsed, false);
    }
    
    /**
     * Highlights mentions in the message text.
     */
    private static Component highlightMentions(String raw, MentionParser.MentionParseResult parsed, boolean recipientOnly) {
        MutableComponent result = Component.empty();
        int lastEnd = 0;
        
        // Create a list of all mention positions
        java.util.List<MentionPosition> positions = new java.util.ArrayList<>();
        
        for (MentionParser.MentionToken token : parsed.tokens()) {
            String searchText = token.isAtMention() ? "@" + token.name() : token.name();
            
            // Find all occurrences of this mention
            Pattern pattern = createSearchPattern(searchText, Config.mentionWordBoundary, Config.mentionCaseInsensitive);
            Matcher matcher = pattern.matcher(raw);
            
            while (matcher.find()) {
                positions.add(new MentionPosition(matcher.start(), matcher.end(), token));
            }
        }
        
        // Sort positions by start index
        positions.sort(java.util.Comparator.comparingInt(MentionPosition::start));
        
        // Build the component
        for (MentionPosition pos : positions) {
            // Add text before this mention
            if (pos.start() > lastEnd) {
                result.append(Component.literal(raw.substring(lastEnd, pos.start())));
            }
            
            // Add the highlighted mention
            String mentionText = raw.substring(pos.start(), pos.end());
            Component highlightedMention = createHighlightedMention(mentionText, pos.token(), recipientOnly);
            result.append(highlightedMention);
            
            lastEnd = pos.end();
        }
        
        // Add remaining text
        if (lastEnd < raw.length()) {
            result.append(Component.literal(raw.substring(lastEnd)));
        }
        
        return result;
    }
    
    /**
     * Creates a highlighted mention component.
     */
    private static Component createHighlightedMention(String text, MentionParser.MentionToken token, boolean recipientOnly) {
        // Parse the highlight color
        TextColor color;
        try {
            color = TextColor.fromRgb(Integer.parseInt(Config.mentionHighlightColorHex, 16));
        } catch (NumberFormatException e) {
            color = TextColor.fromRgb(0xFF3B30); // Default red
        }
        
        // Create the style
        Style style = Style.EMPTY
            .withColor(color);
        
        // Add hover text if configured
        if (Config.mentionHoverText != null && !Config.mentionHoverText.isEmpty()) {
            style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(Config.mentionHoverText)));
        }
        
        return Component.literal(text).withStyle(style);
    }
    
    /**
     * Creates a search pattern for finding mentions.
     */
    private static Pattern createSearchPattern(String text, boolean wordBoundary, boolean caseInsensitive) {
        String pattern = Pattern.quote(text);
        
        if (wordBoundary) {
            pattern = "\\b" + pattern + "\\b";
        }
        
        int flags = 0;
        if (caseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        
        return Pattern.compile(pattern, flags);
    }
    
    /**
     * Represents a mention position in the text.
     */
    private static record MentionPosition(int start, int end, MentionParser.MentionToken token) {}
}
