package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Client-only biome title overlay.
 *
 * Tracks recently-entered biomes (most-recent-first) and only displays a title when entering
 * a biome that is not present in the recent list.
 */
public final class BiomeTitlesClient {
    private BiomeTitlesClient() {
    }

    public record Settings(
            boolean enabled,
            int recentSize,
            int showTicks,
            int fadeTicks,
            float scale,
            int offsetX,
            int offsetY,
            boolean shadow,
            Slot slot,
            Set<Identifier> disabledBiomeIds,
            Set<TagKey<Biome>> disabledBiomeTags
    ) {
        public Settings {
            if (recentSize < 1) recentSize = 1;
            if (recentSize > 50) recentSize = 50;
            if (showTicks < 0) showTicks = 0;
            if (fadeTicks < 0) fadeTicks = 0;
            if (scale <= 0.0f) scale = 1.0f;
            if (slot == null) slot = Slot.SUBTITLE;
            if (disabledBiomeIds == null) disabledBiomeIds = Set.of();
            if (disabledBiomeTags == null) disabledBiomeTags = Set.of();
        }

        public int fadeInTicks() {
            return fadeTicks;
        }

        public int fadeOutTicks() {
            return fadeTicks;
        }

        public int totalTicks() {
            return fadeInTicks() + showTicks + fadeOutTicks();
        }
    }

    public enum Slot {
        TITLE,
        SUBTITLE
    }

    private static final Deque<Identifier> recentBiomes = new ArrayDeque<>();

    private static Identifier lastBiomeId;
    private static Object lastLevelIdentity;

    private static Component activeTitle;
    private static int activeAgeTicks;

    private static Identifier pendingBiomeId;

    // On join, the client player can exist briefly at an incorrect position / unloaded chunk,
    // which can cause the biome query to return a misleading default (often plains).
    private static int suppressDetectionTicks;

    private static boolean isReliableClientChunk(Minecraft mc, BlockPos pos) {
        if (mc == null || mc.level == null || pos == null) return false;
        if (!mc.level.hasChunkAt(pos)) return false;

        // In some situations (e.g. teleporting to unloaded chunks), the client can temporarily
        // have a placeholder chunk that reports a default biome (commonly plains). Avoid sampling
        // biomes from those placeholders.
        try {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            Object chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
            return chunk != null && !(chunk instanceof EmptyLevelChunk);
        } catch (Throwable ignored) {
            // If the chunk-source API changes, fall back to the coarse hasChunkAt check.
            return true;
        }
    }

    public static void tick(Minecraft mc, Settings settings) {
        if (mc == null) {
            return;
        }

        if (!settings.enabled()) {
            clearActive();
            pendingBiomeId = null;
            return;
        }

        if (mc.level == null || mc.player == null) {
            resetAll();
            return;
        }

        Object levelIdentity = mc.level;
        if (lastLevelIdentity != levelIdentity) {
            resetAll();
            lastLevelIdentity = levelIdentity;
            suppressDetectionTicks = 20;
            return;
        }

        if (suppressDetectionTicks > 0) {
            suppressDetectionTicks--;
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        if (!isReliableClientChunk(mc, playerPos)) {
            // Wait until the local chunk is genuinely loaded to avoid sampling a biome from an
            // unloaded/placeholder area.
            return;
        }

        Holder<Biome> biomeHolder = mc.level.getBiome(playerPos);
        Identifier biomeId = biomeHolder
            .unwrapKey()
            .map(net.minecraft.resources.ResourceKey::identifier)
            .orElse(null);

        if (biomeId != null && !Objects.equals(biomeId, lastBiomeId)) {
            lastBiomeId = biomeId;

            // Some biomes (e.g. tiny transition biomes like rivers/beaches) can be noisy or
            // undesirable for titles; allow disabling them via config.
            if (!isDisabled(biomeHolder, biomeId, settings)) {
                boolean wasRecent = recentBiomes.remove(biomeId);
                recentBiomes.addFirst(biomeId);
                trimRecent(settings.recentSize());

                if (!wasRecent) {
                    if (activeTitle == null) {
                        startTitle(biomeId, settings);
                    } else {
                        // Queue the next biome title until the current transition fully completes.
                        pendingBiomeId = biomeId;
                    }
                }
            }
        }

        if (activeTitle != null) {
            activeAgeTicks++;
            if (activeAgeTicks > settings.totalTicks()) {
                clearActive();

                if (pendingBiomeId != null) {
                    Identifier next = pendingBiomeId;
                    pendingBiomeId = null;
                    startTitle(next, settings);
                }
            }
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker, Settings settings) {
        if (!settings.enabled()) return;
        if (activeTitle == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        float partial = deltaTracker != null ? deltaTracker.getGameTimeDeltaTicks() : 0.0f;
        float age = activeAgeTicks + partial;

        float alpha = computeAlpha(age, settings);
        if (alpha <= 0.0f) return;

        int a = (int) (alpha * 255.0f);
        if (a < 0) a = 0;
        if (a > 255) a = 255;

        int color = (a << 24) | 0xFFFFFF;

        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        // Traveler's Titles-like placement: near top center, with configurable offsets.
        float baseX = screenWidth / 2.0f;
        float baseY = screenHeight / 4.0f;
        if (settings.slot() == Slot.SUBTITLE) {
            baseY += 12.0f;
        }

        float scale = settings.scale();
        int drawX = (int) ((baseX + settings.offsetX()) / scale);
        int drawY = (int) ((baseY + settings.offsetY()) / scale);

        Font font = mc.font;
        int width = font.width(activeTitle);

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, activeTitle, drawX - (width / 2), drawY, color, settings.shadow());
        graphics.pose().popMatrix();
    }

    private static void startTitle(Identifier biomeId, Settings settings) {
        activeTitle = titleForBiome(biomeId);
        activeAgeTicks = 0;

        // If total duration is zero, suppress immediately.
        if (settings.totalTicks() == 0) {
            clearActive();
        }
    }

    private static Component titleForBiome(Identifier biomeId) {
        if (biomeId == null) {
            return Component.literal("Unknown biome");
        }

        String translationKey = Util.makeDescriptionId("biome", biomeId);
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }

        // Many modded biomes (especially datapack-provided worldgen like Terralith) don't ship language
        // entries for biome names. Fall back to a readable title-cased form of the biome path.
        String pretty = prettyBiomeName(biomeId);
        return Component.literal(pretty != null ? pretty : biomeId.toString());
    }

    private static String prettyBiomeName(Identifier biomeId) {
        String fullPath = biomeId.getPath();
        if (fullPath == null || fullPath.isEmpty()) {
            return null;
        }

        // Some datapack-provided worldgen (e.g. Terralith) uses folder-like biome paths such as
        // "cave/fungal_caves". Treat the folder as a grouping hint and only display the final
        // segment so we don't show titles like "Cave Fungal Caves".
        String path = fullPath;
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < fullPath.length()) {
            path = fullPath.substring(lastSlash + 1);
        }

        // Convert identifiers like "forested_highlands" into "Forested Highlands".
        String[] parts = path.split("[_\\-]+", -1);
        StringBuilder out = new StringBuilder();

        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }

            if (out.length() > 0) {
                out.append(' ');
            }

            String lower = part.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                out.append(lower, 1, lower.length());
            }
        }

        return out.length() == 0 ? null : out.toString();
    }

    private static float computeAlpha(float age, Settings settings) {
        int fadeIn = settings.fadeInTicks();
        int show = settings.showTicks();
        int fadeOut = settings.fadeOutTicks();

        float t = age;
        if (t < 0.0f) return 0.0f;

        if (fadeIn > 0 && t < fadeIn) {
            return t / fadeIn;
        }

        float afterFadeIn = t - fadeIn;
        if (afterFadeIn < 0) afterFadeIn = 0;

        if (afterFadeIn < show) {
            return 1.0f;
        }

        float afterShow = afterFadeIn - show;
        if (fadeOut > 0 && afterShow < fadeOut) {
            return 1.0f - (afterShow / fadeOut);
        }

        return 0.0f;
    }

    private static void trimRecent(int size) {
        while (recentBiomes.size() > size) {
            recentBiomes.removeLast();
        }
    }

    private static void clearActive() {
        activeTitle = null;
        activeAgeTicks = 0;
    }

    private static boolean isDisabled(Holder<Biome> biomeHolder, Identifier biomeId, Settings settings) {
        if (biomeId != null && settings.disabledBiomeIds().contains(biomeId)) {
            return true;
        }

        for (TagKey<Biome> tag : settings.disabledBiomeTags()) {
            if (biomeHolder.is(tag)) {
                return true;
            }
        }

        return false;
    }

    private static void resetAll() {
        clearActive();
        recentBiomes.clear();
        lastBiomeId = null;
        lastLevelIdentity = null;
        pendingBiomeId = null;
        suppressDetectionTicks = 0;
    }
}
