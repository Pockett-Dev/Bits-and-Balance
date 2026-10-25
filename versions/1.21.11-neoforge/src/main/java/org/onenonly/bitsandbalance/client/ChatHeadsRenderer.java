package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.ClientConfig;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles rendering of player head icons in chat messages.
 * This adds a small player skin face before usernames in the chat.
 */
public class ChatHeadsRenderer {
    
    // Regex patterns for extracting player names from different message types
    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile("^<([^>]+)>");
    private static final Pattern JOIN_LEAVE_PATTERN = Pattern.compile("^([\\w]+) (joined|left) the game");
    private static final Pattern DEATH_PATTERN = Pattern.compile("^([\\w]+) (was|died|fell|drowned|burned|suffocated|blew|hit|shot|killed|slain|pummeled|withered|starved|squashed|experienced|went|walked|tried|discovered|got|froze|didn't|swam|doomed)");
    private static final Pattern ADVANCEMENT_PATTERN = Pattern.compile("^([\\w]+) has (made|completed|reached)");
    
    // Cache for player skin locations to avoid repeated lookups
    private static final Map<String, Identifier> skinCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    
    // Default skins for when player skin is not available
    private static final Identifier STEVE_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private static final Identifier ALEX_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");
    
    /**
     * Renders a player head icon at the specified position.
     * 
     * @param graphics The GuiGraphics context
     * @param playerName The name of the player whose head to render
     * @param x X position to render at
     * @param y Y position to render at
     * @param size Size of the head icon in pixels
     * @param alpha Alpha transparency value (0.0 - 1.0)
     */
    public static void renderChatHead(GuiGraphics graphics, String playerName, int x, int y, int size, float alpha) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        
        Identifier skinLocation = getPlayerSkinLocation(playerName);
        if (skinLocation == null) {
            return;
        }

        int color = ((int)(alpha * 255.0F) << 24) | 0xFFFFFF;
        
        // Save the current state
        graphics.pose().pushMatrix();
        
        // Render the base skin layer (face)
        // UV coordinates for the face: (8, 8) with size 8x8 on a 64x64 texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinLocation, x, y, 8.0F, 8.0F, size, size, 8, 8, 64, 64, color);
        
        // Render the overlay layer (hat/accessories)
        // UV coordinates for the overlay: (40, 8) with size 8x8 on a 64x64 texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinLocation, x, y, 40.0F, 8.0F, size, size, 8, 8, 64, 64, color);
        
        // Restore the previous state
        graphics.pose().popMatrix();
    }
    
    /**
     * Extracts a player name from a chat message text.
     * Tries multiple patterns to match different message types.
     * 
     * @param messageText The chat message text
     * @return The player name if found, null otherwise
     */
    @Nullable
    public static String extractPlayerName(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }
        
        // Strip color codes and formatting (§ followed by any character)
        String cleanText = messageText.replaceAll("§.", "");
        
        // Try standard chat format first (most common)
        Matcher chatMatcher = PLAYER_CHAT_PATTERN.matcher(cleanText);
        if (chatMatcher.find()) {
            return chatMatcher.group(1);
        }
        
        // Try join/leave messages
        Matcher joinLeaveMatcher = JOIN_LEAVE_PATTERN.matcher(cleanText);
        if (joinLeaveMatcher.find()) {
            return joinLeaveMatcher.group(1);
        }
        
        // Try death messages
        Matcher deathMatcher = DEATH_PATTERN.matcher(cleanText);
        if (deathMatcher.find()) {
            return deathMatcher.group(1);
        }
        
        // Try advancement messages
        Matcher advancementMatcher = ADVANCEMENT_PATTERN.matcher(cleanText);
        if (advancementMatcher.find()) {
            return advancementMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Gets the skin texture location for a player.
     * Uses caching to improve performance.
     * 
     * @param playerName The player's name
     * @return The ResourceLocation of the player's skin, or a default skin if not found
     */
    @Nullable
    private static Identifier getPlayerSkinLocation(String playerName) {
        // Check cache first
        if (skinCache.containsKey(playerName)) {
            return skinCache.get(playerName);
        }
        
        // Try to get the player info from the client
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return useDefaultSkin(playerName);
        }
        
        Collection<PlayerInfo> players = mc.getConnection().getOnlinePlayers();
        for (PlayerInfo playerInfo : players) {
            if (playerInfo.getProfile().name().equals(playerName)) {
                Identifier skin = playerInfo.getSkin().body().texturePath();
                cacheSkinLocation(playerName, skin);
                return skin;
            }
        }
        
        // Player not found in online players, use default skin
        return useDefaultSkin(playerName);
    }
    
    /**
     * Caches a player's skin location.
     * Implements simple size-based cache eviction.
     * 
     * @param playerName The player's name
     * @param skinLocation The skin texture location
     */
    private static void cacheSkinLocation(String playerName, Identifier skinLocation) {
        // Simple cache size management: clear oldest entries if too large
        if (skinCache.size() >= MAX_CACHE_SIZE) {
            // Clear half the cache (simple strategy)
            int toRemove = MAX_CACHE_SIZE / 2;
            skinCache.keySet().stream()
                    .limit(toRemove)
                    .toList()
                    .forEach(skinCache::remove);
        }
        
        skinCache.put(playerName, skinLocation);
    }
    
    /**
     * Returns a default skin based on the player name.
     * Uses a simple hash to alternate between Steve and Alex skins.
     * 
     * @param playerName The player's name
     * @return A default skin ResourceLocation
     */
    private static Identifier useDefaultSkin(String playerName) {
        // Use name hash to determine which default skin to use
        // This mimics Minecraft's logic for offline players
        boolean useAlex = (playerName.hashCode() & 1) == 1;
        Identifier defaultSkin = useAlex ? ALEX_SKIN : STEVE_SKIN;
        
        cacheSkinLocation(playerName, defaultSkin);
        return defaultSkin;
    }
    
    /**
     * Clears the skin cache for a specific player.
     * Useful when a player leaves the game.
     * 
     * @param playerName The player's name
     */
    public static void clearPlayerCache(String playerName) {
        skinCache.remove(playerName);
    }
    
    /**
     * Clears the entire skin cache.
     * Useful for resource cleanup or when switching servers.
     */
    public static void clearAllCache() {
        skinCache.clear();
    }
    
    /**
     * Checks if a message should have a chat head rendered.
     * This is a quick check to avoid unnecessary processing.
     * 
     * @param messageText The chat message text
     * @return true if the message appears to contain a player name
     */
    public static boolean shouldRenderChatHead(String messageText) {
        if (!ClientConfig.enableChatHeads) {
            return false;
        }
        
        if (messageText == null || messageText.isEmpty()) {
            return false;
        }
        
        // Strip color codes for clean checking
        String cleanText = messageText.replaceAll("§.", "");
        
        // Quick check: does the message start with common player message indicators?
        return cleanText.startsWith("<") || 
               cleanText.contains(" joined the game") || 
               cleanText.contains(" left the game") ||
               cleanText.contains(" has made the advancement") ||
               cleanText.contains(" has completed the challenge") ||
               cleanText.contains(" was ") ||
               cleanText.contains(" died");
    }
}

