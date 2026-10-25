package org.onenonly.bitsandbalance.common.client;

//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import com.mojang.blaze3d.platform.Window;
//? endif
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? else
import net.minecraft.client.gui.GuiGraphics;
//? endif
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
//? if mc == 1.21.1
import net.minecraft.Util;
//? else
import net.minecraft.util.Util;
//? endif

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
    private static final int STRUCTURE_SUPPRESSION_FADE_TICKS = 8;

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
            boolean suppressInStructures,
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
    private static final Deque<Identifier> pendingBiomes = new ArrayDeque<>();
    private static final Deque<String> recentTitleKeys = new ArrayDeque<>();

    private static Identifier lastBiomeId;
    private static Object lastLevelIdentity;

    private static Identifier activeBiomeId;
    private static String activeTitleKey;
    private static Component activeTitle;
    private static int activeAgeTicks;
    private static int pendingTransitionDelayTicks;
    private static int structureSuppressionFadeTicks;
    private static int structureSuppressionFadeTotalTicks;
    private static float structureSuppressionFadeStartAlpha;

    // On join, the client player can exist briefly at an incorrect position / unloaded chunk,
    // which can cause the biome query to return a misleading default (often plains).
    private static int suppressDetectionTicks;
    private static volatile boolean insideStructure;

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
            pendingBiomes.clear();
            pendingTransitionDelayTicks = 0;
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

        if (settings.suppressInStructures() && insideStructure) {
            rememberCurrentBiomeSilently(mc);
            pendingBiomes.clear();
            pendingTransitionDelayTicks = 0;
            if (activeTitle != null && structureSuppressionFadeTicks <= 0) {
                startStructureSuppressionFade(settings);
            }
            if (structureSuppressionFadeTicks > 0) {
                tickStructureSuppressionFade();
            } else {
                clearActive();
            }
            return;
        }

        if (structureSuppressionFadeTicks > 0) {
            tickStructureSuppressionFade();
            return;
        }

        clearStructureSuppressionFade();

        if (activeTitle == null && !pendingBiomes.isEmpty()) {
            if (pendingTransitionDelayTicks > 0) {
                pendingTransitionDelayTicks--;
            } else {
                Identifier next = pendingBiomes.removeFirst();
                startTitle(next, settings);
            }
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
            //? if mc == 1.21.1
            .map(net.minecraft.resources.ResourceKey::location)
            //? else
            .map(net.minecraft.resources.ResourceKey::identifier)
            //? endif
            .orElse(null);

        if (biomeId != null && !Objects.equals(biomeId, lastBiomeId)) {
            lastBiomeId = biomeId;
            String titleKey = titleKeyForBiome(biomeId);
            boolean disabled = isDisabled(biomeHolder, biomeId, settings);

            // Some biomes (e.g. tiny transition biomes like rivers/beaches) can be noisy or
            // undesirable for titles; allow disabling them via config.
            if (!disabled) {
                boolean inRecent = recentBiomes.contains(biomeId);
                boolean isActive = Objects.equals(biomeId, activeBiomeId);
                boolean inPending = pendingBiomes.contains(biomeId);
                boolean inRecentTitles = recentTitleKeys.contains(titleKey);
                boolean isActiveTitle = Objects.equals(titleKey, activeTitleKey);
                boolean inPendingTitles = pendingContainsTitleKey(titleKey);

                if (!inRecent
                        && !isActive
                        && !inPending
                        && !inRecentTitles
                        && !isActiveTitle
                        && !inPendingTitles) {
                    if (activeTitle == null && pendingTransitionDelayTicks <= 0 && pendingBiomes.isEmpty()) {
                        startTitle(biomeId, settings);
                    } else {
                        // Queue titles until the current transition and its post-fade gap complete.
                        enqueuePendingBiome(biomeId, titleKey);
                    }
                }
            }
        }

        if (activeTitle != null) {
            activeAgeTicks++;
            if (activeAgeTicks > settings.totalTicks()) {
                clearActive();

                if (!pendingBiomes.isEmpty()) {
                    pendingTransitionDelayTicks = pendingStartDelayTicks(settings);
                }
            }
        }
    }

    public static void render(
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            GuiGraphicsExtractor graphics,
            //? else
            GuiGraphics graphics,
            //? endif
            DeltaTracker deltaTracker,
            Settings settings
    ) {
        if (!settings.enabled()) return;
        if (activeTitle == null) return;
        if (settings.suppressInStructures() && insideStructure && structureSuppressionFadeTicks <= 0) {
            startStructureSuppressionFade(settings);
            if (activeTitle == null || structureSuppressionFadeTicks <= 0) return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        float partial = deltaTracker != null ? deltaTracker.getGameTimeDeltaTicks() : 0.0f;
        float age = activeAgeTicks + partial;

        float alpha = structureSuppressionFadeTicks > 0
                ? computeStructureSuppressionAlpha(partial)
                : computeAlpha(age, settings);
        if (alpha <= 0.0f) return;

        int a = (int) (alpha * 255.0f);
        if (a < 0) a = 0;
        if (a > 255) a = 255;

        int color = (a << 24) | 0xFFFFFF;

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        //? else
        Window window = mc.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();
        //? endif

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

        //? if mc == 1.21.1
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, activeTitle, drawX - (width / 2), drawY, color, settings.shadow());
        graphics.pose().popPose();
        //? else
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        graphics.text(font, activeTitle, drawX - (width / 2), drawY, color, settings.shadow());
        //? else
        graphics.drawString(font, activeTitle, drawX - (width / 2), drawY, color, settings.shadow());
        //? endif
        graphics.pose().popMatrix();
        //? endif
    }

    private static void startTitle(Identifier biomeId, Settings settings) {
        String titleKey = titleKeyForBiome(biomeId);
        activeBiomeId = biomeId;
        activeTitleKey = titleKey;

        recentBiomes.remove(biomeId);
        recentBiomes.addFirst(biomeId);
        trimRecent(settings.recentSize());

        recentTitleKeys.remove(titleKey);
        recentTitleKeys.addFirst(titleKey);
        trimRecentTitleKeys(settings.recentSize());

        activeTitle = titleForBiome(biomeId);
        activeAgeTicks = 0;
        clearStructureSuppressionFade();

        // If total duration is zero, suppress immediately.
        if (settings.totalTicks() == 0) {
            clearActive();
        }
    }

    private static void startStructureSuppressionFade(Settings settings) {
        float alpha = computeAlpha(activeAgeTicks, settings);
        if (alpha <= 0.0f) {
            clearActive();
            return;
        }

        structureSuppressionFadeTotalTicks = Math.max(
                1,
                Math.min(STRUCTURE_SUPPRESSION_FADE_TICKS, Math.max(1, settings.fadeOutTicks()))
        );
        structureSuppressionFadeTicks = structureSuppressionFadeTotalTicks;
        structureSuppressionFadeStartAlpha = alpha;
    }

    private static void tickStructureSuppressionFade() {
        activeAgeTicks++;
        structureSuppressionFadeTicks--;
        if (structureSuppressionFadeTicks <= 0) {
            clearActive();
        }
    }

    private static float computeStructureSuppressionAlpha(float partial) {
        if (structureSuppressionFadeTotalTicks <= 0) {
            return 0.0f;
        }

        float progress = structureSuppressionFadeTotalTicks - structureSuppressionFadeTicks + partial;
        float alpha = structureSuppressionFadeStartAlpha * (1.0f - progress / structureSuppressionFadeTotalTicks);
        if (alpha < 0.0f) return 0.0f;
        if (alpha > 1.0f) return 1.0f;
        return alpha;
    }

    private static void clearStructureSuppressionFade() {
        structureSuppressionFadeTicks = 0;
        structureSuppressionFadeTotalTicks = 0;
        structureSuppressionFadeStartAlpha = 0.0f;
    }

    private static void enqueuePendingBiome(Identifier biomeId, String titleKey) {
        if (biomeId == null) {
            return;
        }

        if (Objects.equals(biomeId, activeBiomeId)
                || pendingBiomes.contains(biomeId)
                || Objects.equals(titleKey, activeTitleKey)
                || pendingContainsTitleKey(titleKey)) {
            return;
        }

        // Preserve fast biome transitions in arrival order without replaying the same biome twice.
        pendingBiomes.addLast(biomeId);
    }

    private static int pendingStartDelayTicks(Settings settings) {
        // Leave a short blank gap after fade-out so a queued biome title cannot look like it
        // fires during the previous title's final fade frames.
        return Math.max(4, settings.fadeOutTicks() / 2);
    }

    private static boolean pendingContainsTitleKey(String titleKey) {
        if (titleKey == null || titleKey.isEmpty()) {
            return false;
        }

        for (Identifier pendingBiome : pendingBiomes) {
            if (Objects.equals(titleKey, titleKeyForBiome(pendingBiome))) {
                return true;
            }
        }

        return false;
    }

    private static Component titleForBiome(Identifier biomeId) {
        if (biomeId == null) {
            return Component.literal("Unknown biome");
        }

        String translationKey = Util.makeDescriptionId("biome", biomeId);
//? if mc == 26.2 || mc == 26.3
        String translated = I18n.get(translationKey);
        if (!translationKey.equals(translated)) {
            return Component.translatable(translationKey);
        }
//? else
        if (I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }
//? endif

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

    private static void trimRecentTitleKeys(int size) {
        while (recentTitleKeys.size() > size) {
            recentTitleKeys.removeLast();
        }
    }

    private static void clearActive() {
        activeBiomeId = null;
        activeTitleKey = null;
        activeTitle = null;
        activeAgeTicks = 0;
        clearStructureSuppressionFade();
    }

    private static String titleKeyForBiome(Identifier biomeId) {
        return titleForBiome(biomeId).getString();
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

    public static void setInsideStructure(boolean inside) {
        insideStructure = inside;
    }

    public static boolean isInsideStructure() {
        return insideStructure;
    }

    private static void rememberCurrentBiomeSilently(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        BlockPos playerPos = mc.player.blockPosition();
        if (!isReliableClientChunk(mc, playerPos)) {
            return;
        }

        Identifier biomeId = mc.level.getBiome(playerPos)
            .unwrapKey()
            //? if mc == 1.21.1
            .map(net.minecraft.resources.ResourceKey::location)
            //? else
            .map(net.minecraft.resources.ResourceKey::identifier)
            //? endif
            .orElse(null);
        if (biomeId != null) {
            lastBiomeId = biomeId;
        }
    }

    private static void resetAll() {
        clearActive();
        pendingTransitionDelayTicks = 0;
        recentBiomes.clear();
        recentTitleKeys.clear();
        pendingBiomes.clear();
        lastBiomeId = null;
        lastLevelIdentity = null;
        suppressDetectionTicks = 0;
    }
}
