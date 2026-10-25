package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.onenonly.bitsandbalance.client.ChatHeadPixelAlignment;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FabricChatHeadsRenderer {
    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile("^<([^>]+)>");
    private static final Pattern JOIN_LEAVE_PATTERN = Pattern.compile("^([\\w]+) (joined|left) the game");
    private static final Pattern DEATH_PATTERN = Pattern.compile("^([\\w]+) (was|died|fell|drowned|burned|suffocated|blew|hit|shot|killed|slain|pummeled|withered|starved|squashed|experienced|went|walked|tried|discovered|got|froze|didn't|swam|doomed)");
    private static final Pattern ADVANCEMENT_PATTERN = Pattern.compile("^([\\w]+) has (made|completed|reached)");

    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private static final int MAX_MESSAGE_CACHE_SIZE = 256;
    private static final Map<String, String> MESSAGE_HEAD_CACHE = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_MESSAGE_CACHE_SIZE;
        }
    };
    private static final ResourceLocation STEVE_SKIN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_SKIN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");
    private static final float SHADOW_TEXTURE_TINT = 0.42F;
    private static final float SHADOW_ALPHA_MULTIPLIER = 0.55F;

    private FabricChatHeadsRenderer() {
    }

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
            if (FabricClientConfig.chatHeadsShadow) {
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

    public static String extractPlayerName(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }

        String cleanText = stripFormatting(messageText);
        Matcher chatMatcher = PLAYER_CHAT_PATTERN.matcher(cleanText);
        if (chatMatcher.find()) return chatMatcher.group(1);
        Matcher joinLeaveMatcher = JOIN_LEAVE_PATTERN.matcher(cleanText);
        if (joinLeaveMatcher.find()) return joinLeaveMatcher.group(1);
        Matcher deathMatcher = DEATH_PATTERN.matcher(cleanText);
        if (deathMatcher.find()) return deathMatcher.group(1);
        Matcher advancementMatcher = ADVANCEMENT_PATTERN.matcher(cleanText);
        if (advancementMatcher.find()) return advancementMatcher.group(1);
        return null;
    }

    public static boolean isAdvancementMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return false;
        }

        return ADVANCEMENT_PATTERN.matcher(stripFormatting(messageText)).find();
    }

    public static void rememberChatHeadOwner(Component message, PlayerInfo senderInfo, boolean allowLocalPlayerFallback) {
        if (message == null) {
            return;
        }

        String messageText = message.getString();
        if (messageText == null || messageText.isBlank()) {
            return;
        }

        String playerName = null;
        if (senderInfo != null && senderInfo.getProfile() != null) {
            playerName = senderInfo.getProfile().getName();
        }

        if (playerName == null || playerName.isBlank()) {
            playerName = extractPlayerName(messageText);
        }

        if ((playerName == null || playerName.isBlank()) && allowLocalPlayerFallback) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getGameProfile() != null) {
                playerName = mc.player.getGameProfile().getName();
            }
        }

        if (playerName == null || playerName.isBlank()) {
            return;
        }

        synchronized (MESSAGE_HEAD_CACHE) {
            MESSAGE_HEAD_CACHE.put(normalizeMessageKey(messageText), playerName);
        }
    }

    public static String resolvePlayerName(String messageText) {
        String rememberedPlayer = getRememberedPlayerName(messageText);
        if (rememberedPlayer != null) {
            return rememberedPlayer;
        }

        return extractPlayerName(messageText);
    }

    public static boolean shouldRenderChatHead(String messageText) {
        if (!FabricClientConfig.enableChatHeads) {
            return false;
        }
        if (messageText == null || messageText.isEmpty()) {
            return false;
        }

        if (getRememberedPlayerName(messageText) != null) {
            return true;
        }

        String cleanText = stripFormatting(messageText);
        return cleanText.startsWith("<")
        || isAdvancementMessage(cleanText);
    }

    private static ResourceLocation getPlayerSkinLocation(String playerName) {
        if (SKIN_CACHE.containsKey(playerName)) {
            return SKIN_CACHE.get(playerName);
        }

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

        return useDefaultSkin(playerName);
    }

    private static void cacheSkinLocation(String playerName, ResourceLocation skinLocation) {
        if (SKIN_CACHE.size() >= MAX_CACHE_SIZE) {
            int toRemove = MAX_CACHE_SIZE / 2;
            SKIN_CACHE.keySet().stream().limit(toRemove).toList().forEach(SKIN_CACHE::remove);
        }

        SKIN_CACHE.put(playerName, skinLocation);
    }

    private static ResourceLocation useDefaultSkin(String playerName) {
        boolean useAlex = (playerName.hashCode() & 1) == 1;
        ResourceLocation defaultSkin = useAlex ? ALEX_SKIN : STEVE_SKIN;
        cacheSkinLocation(playerName, defaultSkin);
        return defaultSkin;
    }

    private static String getRememberedPlayerName(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        String messageKey = normalizeMessageKey(messageText);
        synchronized (MESSAGE_HEAD_CACHE) {
            String exactMatch = MESSAGE_HEAD_CACHE.get(messageKey);
            if (exactMatch != null) {
                return exactMatch;
            }

            // ChatComponent renders each wrapped visual line separately. The
            // first line is a prefix of the original cached message, while a
            // continuation line is not, so only the first line gets the head.
            String wrappedFirstLineMatch = null;
            for (Map.Entry<String, String> entry : MESSAGE_HEAD_CACHE.entrySet()) {
                if (entry.getKey().length() > messageKey.length()
                        && entry.getKey().startsWith(messageKey)) {
                    wrappedFirstLineMatch = entry.getValue();
                }
            }
            return wrappedFirstLineMatch;
        }
    }

    private static String normalizeMessageKey(String value) {
        return stripFormatting(value).trim();
    }

    private static String stripFormatting(String value) {
        return value.replaceAll("\\u00A7.", "");
    }

    private static boolean blitFace(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size, int u, int v, float alpha) {
        return blitFaceTinted(graphics, texture, x, y, size, u, v, 1.0F, 1.0F, 1.0F, alpha);
    }

    private static boolean blitFaceTinted(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size, int u, int v,
                                          float red, float green, float blue, float alpha) {
        if (alpha <= 0.0F) {
            return true;
        }

        try {
            float u0 = u / 64.0F;
            float u1 = (u + 8) / 64.0F;
            float v0 = v / 64.0F;
            float v1 = (v + 8) / 64.0F;
            int x1 = x + size;
            int y1 = y + size;
            Matrix4f matrix = graphics.pose().last().pose();

            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.enableBlend();

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(matrix, x, y1, 0.0F).setUv(u0, v1).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x1, y1, 0.0F).setUv(u1, v1).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x1, y, 0.0F).setUv(u1, v0).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x, y, 0.0F).setUv(u0, v0).setColor(red, green, blue, alpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.disableBlend();
            return true;
        } catch (Throwable ignored) {
            try {
                RenderSystem.disableBlend();
            } catch (Throwable ignoredAgain) {
            }
            return false;
        }
    }
}
