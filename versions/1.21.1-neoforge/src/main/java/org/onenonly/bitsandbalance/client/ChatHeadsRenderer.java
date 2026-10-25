package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.onenonly.bitsandbalance.ClientConfig;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;
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
    private static final Map<String, ResourceLocation> skinCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private static volatile boolean colorBlitResolved;
    private static volatile Method colorBlitMethod;
    
    // Default skins for when player skin is not available
    private static final ResourceLocation STEVE_SKIN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_SKIN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");
    private static final float SHADOW_TEXTURE_TINT = 0.42F;
    private static final float SHADOW_ALPHA_MULTIPLIER = 0.55F;
    
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
        if (graphics == null || playerName == null || playerName.isEmpty()) {
            return;
        }

        ResourceLocation skinLocation = getPlayerSkinLocation(playerName);
        if (skinLocation == null) {
            return;
        }

        float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int renderY = y - 1;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, ChatHeadPixelAlignment.baselineOffsetY(), 0.0F);
            if (ClientConfig.chatHeadsShadow) {
                blitFaceTinted(graphics, skinLocation, x + 1, renderY + 1, size, 8, 8, SHADOW_TEXTURE_TINT, SHADOW_TEXTURE_TINT, SHADOW_TEXTURE_TINT, clampedAlpha * SHADOW_ALPHA_MULTIPLIER);
                blitFaceTinted(graphics, skinLocation, x + 1, renderY + 1, size, 40, 8, SHADOW_TEXTURE_TINT, SHADOW_TEXTURE_TINT, SHADOW_TEXTURE_TINT, clampedAlpha * SHADOW_ALPHA_MULTIPLIER);
            }
            if (!blitFace(graphics, skinLocation, x, renderY, size, 8, 8, clampedAlpha)) {
                return;
            }
            blitFace(graphics, skinLocation, x, renderY, size, 40, 8, clampedAlpha);
        } finally {
            try {
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } catch (Throwable ignored) {
            }
            graphics.pose().popPose();
        }
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
        
        String cleanText = stripFormatting(messageText);
        
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
    private static ResourceLocation getPlayerSkinLocation(String playerName) {
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
            if (playerInfo.getProfile().getName().equals(playerName)) {
                ResourceLocation skin = playerInfo.getSkin().texture();
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
    private static void cacheSkinLocation(String playerName, ResourceLocation skinLocation) {
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
    private static ResourceLocation useDefaultSkin(String playerName) {
        // Use name hash to determine which default skin to use
        // This mimics Minecraft's logic for offline players
        boolean useAlex = (playerName.hashCode() & 1) == 1;
        ResourceLocation defaultSkin = useAlex ? ALEX_SKIN : STEVE_SKIN;
        
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
        
        String cleanText = stripFormatting(messageText);
        
        // Quick check: does the message start with common player message indicators?
        return cleanText.startsWith("<") || 
               cleanText.contains(" joined the game") || 
               cleanText.contains(" left the game") ||
               cleanText.contains(" has made the advancement") ||
               cleanText.contains(" has completed the challenge") ||
               cleanText.contains(" was ") ||
               cleanText.contains(" died");
    }

    private static String stripFormatting(String value) {
        return value.replaceAll("\\u00A7.", "");
    }

    private static boolean blitFace(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size, int u, int v, float alpha) {
        return blitFaceTinted(graphics, texture, x, y, size, u, v, 1.0F, 1.0F, 1.0F, alpha);
    }

    private static boolean blitFaceTinted(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size, int u, int v,
                                          float red, float green, float blue, float alpha) {
        try {
            Method method = bitsandbalance$getColorBlitMethod(graphics);
            if (method != null) {
                method.invoke(
                        graphics,
                        texture,
                        x,
                        x + size,
                        y,
                        y + size,
                        0,
                        u / 64.0F,
                        (u + 8) / 64.0F,
                        v / 64.0F,
                        (v + 8) / 64.0F,
                        red,
                        green,
                        blue,
                        alpha
                );
                return true;
            }
        } catch (Throwable ignored) {
        }

        try {
            graphics.setColor(red, green, blue, alpha);
            graphics.blit(texture, x, y, (float) u, (float) v, size, size, 64, 64);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method bitsandbalance$getColorBlitMethod(GuiGraphics graphics) {
        if (!colorBlitResolved) {
            colorBlitResolved = true;
            for (Method method : graphics.getClass().getDeclaredMethods()) {
                if (!method.getName().equals("innerBlit")) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 14 || parameterTypes[0] != ResourceLocation.class) {
                    continue;
                }

                boolean matches = true;
                for (int i = 1; i <= 5; i++) {
                    if (parameterTypes[i] != int.class) {
                        matches = false;
                        break;
                    }
                }
                for (int i = 6; i < parameterTypes.length; i++) {
                    if (parameterTypes[i] != float.class) {
                        matches = false;
                        break;
                    }
                }

                if (matches) {
                    method.setAccessible(true);
                    colorBlitMethod = method;
                    break;
                }
            }
        }

        return colorBlitMethod;
    }
}

