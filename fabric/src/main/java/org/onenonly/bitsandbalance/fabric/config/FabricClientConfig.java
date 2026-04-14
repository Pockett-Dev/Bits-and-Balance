package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-specific configuration for the Fabric port of BitsAndBalance.
 * Covers rendering, UI, controls, and client-side mechanics.
 */
public final class FabricClientConfig {
    private FabricClientConfig() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-client-config");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "bitsandbalance-client.json";

    // ========== Night Vision Improvement ==========
    public static boolean enableNightVisionFade = true;
    public static int nightVisionFadeSeconds = 10;

    // ========== See Held Item ==========
    public static boolean enableShowHeldItemWhenRiding = true;

    // ========== Elder Guardian Appearance ==========
    public static boolean disableGuardianJumpscare = true;
    public static boolean playGuardianJumpscareSound = true;

    // ========== Auto Walk / Controls ==========
    public static boolean enableAutoWalkKey = true;

    // ========== Experience / Sounds ==========
    public static boolean enableLevel30OldSound = true;

    // ========== Screenshots ==========
    public static boolean enableScreenshotsToClipboard = true;

    // ========== Custom Splash Text ==========
    public static boolean enableCustomSplashTexts = true;
    public static int customSplashMultiplier = 2;

    // ========== World / Experimental Warnings ==========
    public static boolean suppressExperimentalSettingsWarning = true;

    // ========== Log Filtering ==========
    public static boolean suppressFarChunkErrors = true;
    public static boolean suppressRemovedRecipeBookWarnings = true;

    // ========== Biome Item Tinting ==========
    public static boolean biomeItemTintEnabled = true;
        /** Block/item IDs (resource locations) that should never be biome-tinted. */
        public static List<String> biomeItemTintDisabledItems = new ArrayList<>(List.of(
            "minecraft:sugar_cane"
        ));
        /** Parsed set derived from {@link #biomeItemTintDisabledItems}. */
        public static Set<Identifier> biomeItemTintDisabledItemIds = parseIds(biomeItemTintDisabledItems);

    // ========== Soul Fire Overlay ==========
    public static boolean soulFireOverlayEnabled = true;
    public static boolean soulFireCandleFlamesEnabled = true;

    // ========== Biome Titles ==========
    public static boolean biomeTitlesEnabled = true;
    public static int biomeTitlesRecentSize = 7;
    public static int biomeTitlesShowTicks = 60; // 3 seconds
    public static int biomeTitlesFadeTicks = 20; // 1 second
    public static double biomeTitlesScale = 2.0D;
    public static int biomeTitlesOffsetX = 0;
    public static int biomeTitlesOffsetY = 0;
    public static boolean biomeTitlesShadow = true;
    /** "title" or "subtitle" (default). */
    public static String biomeTitlesSlot = "subtitle";
        /** Biome IDs that should never display a title (resource locations). */
        public static List<String> biomeTitlesDisabledBiomes = new ArrayList<>(List.of(
                "#minecraft:is_river",
            "minecraft:river",
            "minecraft:frozen_river",
            "minecraft:beach",
            "minecraft:snowy_beach",
            "minecraft:stony_shore"
        ));
            /** Parsed sets derived from {@link #biomeTitlesDisabledBiomes}. */
            public static Set<Identifier> biomeTitlesDisabledBiomeIds = parseBiomeSelectors(biomeTitlesDisabledBiomes).ids();
            public static Set<TagKey<Biome>> biomeTitlesDisabledBiomeTags = parseBiomeSelectors(biomeTitlesDisabledBiomes).tags();

    // ========== Chat Heads ==========
    public static boolean enableChatHeads = true;
    public static int chatHeadSize = 8;
    public static int chatHeadOffset = 0;

    // ========== Keybind Debugging ==========
    // If true, logs a short burst of key-rebind events when selecting a keybind in the Controls UI.
    // Disabled by default to avoid noisy logs.
    public static boolean debugKeybindRebindLogging = false;

    // ========== Usage Ticker ==========
    public static boolean usageTickerEnabled = true;
    public static boolean usageTickerInvert = false;
    public static int usageTickerOffsetX = 0;
    public static int usageTickerOffsetY = 0;
    public static boolean usageTickerDebug = false;

    // ========== Leaf Litter / Leaf Pile Tint ==========
    public static boolean leafLitterTintEnabled = true;
    public static String leafLitterTintSource = "foliage";
    public static boolean leafLitterTintClampEnabled = false;
    public static String leafLitterTintGlobalMinColor = "";
    public static String leafLitterTintGlobalMaxColor = "";
    public static List<String> leafLitterTintBiomeRanges = new ArrayList<>();

    public static void init() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    applyConfig(data);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load client config {} (continuing with defaults)", configPath, e);
            }
        }
        save();

        LOGGER.debug("Client config path: {}", configPath);

        // Apply log filter settings
        org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.setEnabled(suppressFarChunkErrors);
    }

    private static void applyConfig(ConfigData data) {
        if (data.nightVision != null) {
            enableNightVisionFade = data.nightVision.enableFade;
            nightVisionFadeSeconds = data.nightVision.fadeSeconds;
        }
        if (data.seeHeldItem != null) {
            enableShowHeldItemWhenRiding = data.seeHeldItem.enableShowHeldItemWhenRiding;
        }
        if (data.guardian != null) {
            disableGuardianJumpscare = data.guardian.disableJumpscare;
            playGuardianJumpscareSound = data.guardian.playSound;
        }
        if (data.autoWalk != null) {
            enableAutoWalkKey = data.autoWalk.enableAutoWalkKey;
        }
        if (data.experience != null) {
            enableLevel30OldSound = data.experience.enableLevel30OldSound;
        }
        if (data.screenshots != null) {
            enableScreenshotsToClipboard = data.screenshots.enableScreenshotsToClipboard;
        }
        if (data.splashText != null) {
            enableCustomSplashTexts = data.splashText.enableCustomSplashTexts;
            customSplashMultiplier = data.splashText.customSplashMultiplier;
        }
        if (data.worldWarnings != null) {
            suppressExperimentalSettingsWarning = data.worldWarnings.suppressExperimentalSettingsWarning;
        }
        if (data.logFiltering != null) {
            suppressFarChunkErrors = data.logFiltering.suppressFarChunkErrors;
            suppressRemovedRecipeBookWarnings = data.logFiltering.suppressRemovedRecipeBookWarnings;
        }
        if (data.biomeItemTint != null) {
            biomeItemTintEnabled = data.biomeItemTint.enabled;
            if (data.biomeItemTint.disabledItems != null) {
                biomeItemTintDisabledItems = new ArrayList<>(data.biomeItemTint.disabledItems);
            }
        }

        biomeItemTintDisabledItemIds = parseIds(biomeItemTintDisabledItems);

        if (data.soulFireOverlay != null) {
            soulFireOverlayEnabled = data.soulFireOverlay.enabled;
            soulFireCandleFlamesEnabled = data.soulFireOverlay.candleFlames;
        }

        if (data.biomeTitles != null) {
            biomeTitlesEnabled = data.biomeTitles.enabled;
            biomeTitlesRecentSize = data.biomeTitles.recentSize;
            biomeTitlesShowTicks = data.biomeTitles.showSeconds * 20;
            biomeTitlesFadeTicks = data.biomeTitles.fadeSeconds * 20;
            biomeTitlesScale = data.biomeTitles.scale;
            biomeTitlesOffsetX = data.biomeTitles.offsetX;
            biomeTitlesOffsetY = data.biomeTitles.offsetY;
            biomeTitlesShadow = data.biomeTitles.shadow;
            biomeTitlesSlot = data.biomeTitles.slot;
            if (data.biomeTitles.disabledBiomes != null) {
                biomeTitlesDisabledBiomes = new ArrayList<>(data.biomeTitles.disabledBiomes);
            }
            DisabledBiomeSelectors parsed = parseBiomeSelectors(biomeTitlesDisabledBiomes);
            biomeTitlesDisabledBiomeIds = parsed.ids();
            biomeTitlesDisabledBiomeTags = parsed.tags();
        }
        if (data.chatHeads != null) {
            enableChatHeads = data.chatHeads.enableChatHeads;
            chatHeadSize = data.chatHeads.chatHeadSize;
            chatHeadOffset = data.chatHeads.chatHeadOffset;
        }
        if (data.keybindModifiers != null) {
            debugKeybindRebindLogging = data.keybindModifiers.debugRebindLogging;
        }

        if (data.usageTicker != null) {
            usageTickerEnabled = data.usageTicker.enabled;
            usageTickerInvert = data.usageTicker.invert;
            usageTickerOffsetX = data.usageTicker.offsetX;
            usageTickerOffsetY = data.usageTicker.offsetY;
            usageTickerDebug = data.usageTicker.debug;
        }
        if (data.leafLitterTint != null) {
            leafLitterTintEnabled = data.leafLitterTint.enabled;
            leafLitterTintSource = data.leafLitterTint.source;
            leafLitterTintClampEnabled = data.leafLitterTint.clampEnabled;
            leafLitterTintGlobalMinColor = data.leafLitterTint.globalMinColor;
            leafLitterTintGlobalMaxColor = data.leafLitterTint.globalMaxColor;
            if (data.leafLitterTint.biomeRanges != null) {
                leafLitterTintBiomeRanges = new ArrayList<>(data.leafLitterTint.biomeRanges);
            }
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.nightVision = new NightVisionConfig();
        data.nightVision.enableFade = enableNightVisionFade;
        data.nightVision.fadeSeconds = nightVisionFadeSeconds;

        data.seeHeldItem = new SeeHeldItemConfig();
        data.seeHeldItem.enableShowHeldItemWhenRiding = enableShowHeldItemWhenRiding;

        data.guardian = new GuardianConfig();
        data.guardian.disableJumpscare = disableGuardianJumpscare;
        data.guardian.playSound = playGuardianJumpscareSound;

        data.autoWalk = new AutoWalkConfig();
        data.autoWalk.enableAutoWalkKey = enableAutoWalkKey;

        data.experience = new ExperienceConfig();
        data.experience.enableLevel30OldSound = enableLevel30OldSound;

        data.screenshots = new ScreenshotsConfig();
        data.screenshots.enableScreenshotsToClipboard = enableScreenshotsToClipboard;

        data.splashText = new SplashTextConfig();
        data.splashText.enableCustomSplashTexts = enableCustomSplashTexts;
        data.splashText.customSplashMultiplier = customSplashMultiplier;

        data.worldWarnings = new WorldWarningsConfig();
        data.worldWarnings.suppressExperimentalSettingsWarning = suppressExperimentalSettingsWarning;

        data.logFiltering = new LogFilteringConfig();
        data.logFiltering.suppressFarChunkErrors = suppressFarChunkErrors;
        data.logFiltering.suppressRemovedRecipeBookWarnings = suppressRemovedRecipeBookWarnings;

        data.biomeItemTint = new BiomeItemTintConfig();
        data.biomeItemTint.enabled = biomeItemTintEnabled;
        data.biomeItemTint.disabledItems = biomeItemTintDisabledItems;

        data.soulFireOverlay = new SoulFireOverlayConfig();
        data.soulFireOverlay.enabled = soulFireOverlayEnabled;
        data.soulFireOverlay.candleFlames = soulFireCandleFlamesEnabled;

        data.biomeTitles = new BiomeTitlesConfig();
        data.biomeTitles.enabled = biomeTitlesEnabled;
        data.biomeTitles.recentSize = biomeTitlesRecentSize;
        data.biomeTitles.showSeconds = Math.max(0, biomeTitlesShowTicks / 20);
        data.biomeTitles.fadeSeconds = Math.max(0, biomeTitlesFadeTicks / 20);
        data.biomeTitles.scale = biomeTitlesScale;
        data.biomeTitles.offsetX = biomeTitlesOffsetX;
        data.biomeTitles.offsetY = biomeTitlesOffsetY;
        data.biomeTitles.shadow = biomeTitlesShadow;
        data.biomeTitles.slot = biomeTitlesSlot;
        data.biomeTitles.disabledBiomes = biomeTitlesDisabledBiomes;

        data.chatHeads = new ChatHeadsConfig();
        data.chatHeads.enableChatHeads = enableChatHeads;
        data.chatHeads.chatHeadSize = chatHeadSize;
        data.chatHeads.chatHeadOffset = chatHeadOffset;

        data.keybindModifiers = new KeybindModifiersConfig();
        data.keybindModifiers.debugRebindLogging = debugKeybindRebindLogging;

        data.usageTicker = new UsageTickerConfig();
        data.usageTicker.enabled = usageTickerEnabled;
        data.usageTicker.invert = usageTickerInvert;
        data.usageTicker.offsetX = usageTickerOffsetX;
        data.usageTicker.offsetY = usageTickerOffsetY;
        data.usageTicker.debug = usageTickerDebug;

        data.leafLitterTint = new LeafLitterTintConfig();
        data.leafLitterTint.enabled = leafLitterTintEnabled;
        data.leafLitterTint.source = leafLitterTintSource;
        data.leafLitterTint.clampEnabled = leafLitterTintClampEnabled;
        data.leafLitterTint.globalMinColor = leafLitterTintGlobalMinColor;
        data.leafLitterTint.globalMaxColor = leafLitterTintGlobalMaxColor;
        data.leafLitterTint.biomeRanges = leafLitterTintBiomeRanges;

        Path configPath = FabricConfigPaths.resolve(FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject existingRoot = FabricJsonConfigFiles.readRoot(configPath, GSON);

            JsonElement tree = GSON.toJsonTree(data);
            if (!(tree instanceof JsonObject root)) {
                // Should never happen, but avoid writing invalid output.
                return;
            }

            // Root comment
            FabricJsonComments.put(root, "_file", "Bits and Balance (Fabric) client configuration.");
            FabricJsonComments.put(root, "_note", "Descriptions live in the reserved 'description' objects because JSON does not support comments.");

            // Night vision
            JsonObject nightVision = root.getAsJsonObject("nightVision");
            if (nightVision != null) {
                FabricJsonComments.put(nightVision, "enableFade", "If true, Night Vision fades out smoothly instead of cutting off abruptly.");
                FabricJsonComments.put(nightVision, "fadeSeconds", "Fade duration (seconds) once Night Vision is about to end.");
            }

            // See held item
            JsonObject seeHeldItem = root.getAsJsonObject("seeHeldItem");
            if (seeHeldItem != null) {
                FabricJsonComments.put(seeHeldItem, "enableShowHeldItemWhenRiding", "If true, shows your held item while riding (instead of hiding it).");
            }

            // Guardian
            JsonObject guardian = root.getAsJsonObject("guardian");
            if (guardian != null) {
                FabricJsonComments.put(guardian, "disableJumpscare", "If true, disables the Elder Guardian Mining Fatigue appearance effect.");
                FabricJsonComments.put(guardian, "playSound", "If true, still plays the normal Elder Guardian Mining Fatigue sound while the appearance effect is suppressed.");
            }

            // Auto-walk
            JsonObject autoWalk = root.getAsJsonObject("autoWalk");
            if (autoWalk != null) {
                FabricJsonComments.put(autoWalk, "enableAutoWalkKey", "If true, enables the auto-walk keybind (client-side).");
            }

            // Experience
            JsonObject experience = root.getAsJsonObject("experience");
            if (experience != null) {
                FabricJsonComments.put(experience, "enableLevel30OldSound", "If true, restores the classic (older) sound when reaching level 30.");
            }

            // Screenshots
            JsonObject screenshots = root.getAsJsonObject("screenshots");
            if (screenshots != null) {
                FabricJsonComments.put(screenshots, "enableScreenshotsToClipboard", "If true, successful in-game screenshots are also copied to the system clipboard and a client-only confirmation message is shown.");
            }

            // Splash text
            JsonObject splashText = root.getAsJsonObject("splashText");
            if (splashText != null) {
                FabricJsonComments.put(splashText, "enableCustomSplashTexts", "If true, enables Bits and Balance custom splash texts on the title screen.");
                FabricJsonComments.put(splashText, "customSplashMultiplier", "Relative weight multiplier for Bits and Balance splashes vs vanilla (higher = more frequent).");
            }

            // World warnings
            JsonObject worldWarnings = root.getAsJsonObject("worldWarnings");
            if (worldWarnings != null) {
                FabricJsonComments.put(worldWarnings, "suppressExperimentalSettingsWarning", "If true, hides the experimental settings warning screen.");
            }

            // Log filtering
            JsonObject logFiltering = root.getAsJsonObject("logFiltering");
            if (logFiltering != null) {
                FabricJsonComments.put(logFiltering, "suppressFarChunkErrors", "If true, suppresses noisy far-chunk related client log errors.");
                FabricJsonComments.put(logFiltering, "suppressRemovedRecipeBookWarnings", "If true, suppresses stale recipe-book warnings for recipes that no longer exist in the current world/modpack.");
            }

            // Biome item tint
            JsonObject biomeItemTint = root.getAsJsonObject("biomeItemTint");
            if (biomeItemTint != null) {
                FabricJsonComments.put(biomeItemTint, "enabled", "If true, certain vanilla foliage/grass items are tinted using the current biome (inventory/hand and dropped items).");
                FabricJsonComments.put(biomeItemTint, "disabledItems", "List of block/item IDs that should never be biome-tinted (resource locations). Default disables sugar cane.");
            }

            // Soul fire overlay
            JsonObject soulFireOverlay = root.getAsJsonObject("soulFireOverlay");
            if (soulFireOverlay != null) {
                FabricJsonComments.put(soulFireOverlay, "enabled", "If true, replaces the on-screen fire overlay with soul-fire sprites when burning from Soul Fire sources.");
                FabricJsonComments.put(soulFireOverlay, "candleFlames", "If true, lit candles placed on soul sand or soul soil use blue soul-fire flame particles.");
            }

            // Biome titles
            JsonObject biomeTitles = root.getAsJsonObject("biomeTitles");
            if (biomeTitles != null) {
                FabricJsonComments.put(biomeTitles, "enabled", "If true, shows a title/subtitle when entering a new biome.");
                FabricJsonComments.put(biomeTitles, "recentSize", "Number of recently visited biomes to track (re-entering one won't re-trigger the title).");
                FabricJsonComments.put(biomeTitles, "showSeconds", "How long (seconds) the biome title stays visible before fading.");
                FabricJsonComments.put(biomeTitles, "fadeSeconds", "Fade-out time (seconds) for biome titles.");
                FabricJsonComments.put(biomeTitles, "scale", "Scale multiplier for biome title text.");
                FabricJsonComments.put(biomeTitles, "offsetX", "Horizontal pixel offset for biome title position.");
                FabricJsonComments.put(biomeTitles, "offsetY", "Vertical pixel offset for biome title position.");
                FabricJsonComments.put(biomeTitles, "shadow", "If true, renders biome titles with text shadow.");
                FabricJsonComments.put(biomeTitles, "slot", "Where to render biome titles: 'title' or 'subtitle'.");
                FabricJsonComments.put(biomeTitles, "disabledBiomes", "List of biome IDs and/or biome tags to never show titles for. Tags are prefixed with '#'.");
            }

            // Chat heads
            JsonObject chatHeads = root.getAsJsonObject("chatHeads");
            if (chatHeads != null) {
                FabricJsonComments.put(chatHeads, "enableChatHeads", "If true, shows player heads in chat next to messages.");
                FabricJsonComments.put(chatHeads, "chatHeadSize", "Chat head size in pixels.");
                FabricJsonComments.put(chatHeads, "chatHeadOffset", "Horizontal offset (pixels) for the chat head relative to default.");
            }

            // Keybind modifiers
            JsonObject keybindModifiers = root.getAsJsonObject("keybindModifiers");
            if (keybindModifiers != null) {
                FabricJsonComments.put(keybindModifiers, "debugRebindLogging", "If true, enables temporary debug logging for keybind rebinding events (noisy).");

                JsonObject existingKeybindModifiers = existingRoot.getAsJsonObject("keybindModifiers");
                if (existingKeybindModifiers != null) {
                    JsonObject storedModifiers = existingKeybindModifiers.getAsJsonObject("modifiers");
                    if (storedModifiers != null) {
                        keybindModifiers.add("modifiers", storedModifiers.deepCopy());
                    }
                }
            }

            // Usage ticker
            JsonObject usageTicker = root.getAsJsonObject("usageTicker");
            if (usageTicker != null) {
                FabricJsonComments.put(usageTicker, "enabled", "If true, shows the usage ticker HUD element.");
                FabricJsonComments.put(usageTicker, "invert", "If true, inverts the usage ticker direction/layout.");
                FabricJsonComments.put(usageTicker, "offsetX", "Horizontal pixel offset for the usage ticker.");
                FabricJsonComments.put(usageTicker, "offsetY", "Vertical pixel offset for the usage ticker.");
                FabricJsonComments.put(usageTicker, "debug", "If true, enables debug info for the usage ticker.");
            }

            // Leaf litter tint
            JsonObject leafLitterTint = root.getAsJsonObject("leafLitterTint");
            if (leafLitterTint != null) {
                FabricJsonComments.put(leafLitterTint, "enabled", "If true, applies biome-based tinting to leaf litter/leaf piles.");
                FabricJsonComments.put(leafLitterTint, "source", "Tint source: 'foliage' or 'grass'.");
                FabricJsonComments.put(leafLitterTint, "clampEnabled", "If true, clamps biome tint into configured min/max colors.");
                FabricJsonComments.put(leafLitterTint, "globalMinColor", "Optional global minimum tint color as hex (e.g., '00FF00'). Empty = no clamp.");
                FabricJsonComments.put(leafLitterTint, "globalMaxColor", "Optional global maximum tint color as hex (e.g., '00FF00'). Empty = no clamp.");
                FabricJsonComments.put(leafLitterTint, "biomeRanges", "Optional biome-specific clamp ranges (format documented in the config UI/README if present).");
            }

            String json = GSON.toJson(root);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            LOGGER.warn("Failed to save client config {}", configPath, e);
        }
    }

    // JSON structure classes
    private static class ConfigData {
        NightVisionConfig nightVision;
        SeeHeldItemConfig seeHeldItem;
        GuardianConfig guardian;
        AutoWalkConfig autoWalk;
        ExperienceConfig experience;
        ScreenshotsConfig screenshots;
        SplashTextConfig splashText;
        WorldWarningsConfig worldWarnings;
        LogFilteringConfig logFiltering;
        BiomeItemTintConfig biomeItemTint;
        SoulFireOverlayConfig soulFireOverlay;
        BiomeTitlesConfig biomeTitles;
        ChatHeadsConfig chatHeads;
        KeybindModifiersConfig keybindModifiers;
        UsageTickerConfig usageTicker;
        LeafLitterTintConfig leafLitterTint;
    }

    private static class NightVisionConfig {
        boolean enableFade = true;
        int fadeSeconds = 10;
    }

    private static class SeeHeldItemConfig {
        boolean enableShowHeldItemWhenRiding = true;
    }

    private static class GuardianConfig {
        boolean disableJumpscare = true;
        boolean playSound = true;
    }

    private static class AutoWalkConfig {
        boolean enableAutoWalkKey = true;
    }

    private static class ExperienceConfig {
        boolean enableLevel30OldSound = true;
    }

    private static class ScreenshotsConfig {
        boolean enableScreenshotsToClipboard = true;
    }

    private static class SplashTextConfig {
        boolean enableCustomSplashTexts = true;
        int customSplashMultiplier = 2;
    }

    private static class WorldWarningsConfig {
        boolean suppressExperimentalSettingsWarning = true;
    }

    private static class LogFilteringConfig {
        boolean suppressFarChunkErrors = true;
        boolean suppressRemovedRecipeBookWarnings = true;
    }

    private static class BiomeItemTintConfig {
        boolean enabled = true;
        List<String> disabledItems = new ArrayList<>(List.of(
                "minecraft:sugar_cane"
        ));
    }

    private static Set<Identifier> parseIds(List<String> values) {
        if (values == null || values.isEmpty()) return Set.of();

        HashSet<Identifier> ids = new HashSet<>();
        for (String raw : values) {
            if (raw == null || raw.isBlank()) continue;
            try {
                ids.add(Identifier.parse(raw.trim()));
            } catch (Exception ignored) {
                // Ignore invalid entries.
            }
        }

        return Set.copyOf(ids);
    }

    private static class SoulFireOverlayConfig {
        boolean enabled = true;
        boolean candleFlames = true;
    }

    private static class BiomeTitlesConfig {
        boolean enabled = true;
        int recentSize = 5;
        int showSeconds = 3;
        int fadeSeconds = 1;
        double scale = 2.0D;
        int offsetX = 0;
        int offsetY = 0;
        boolean shadow = true;
        String slot = "subtitle";
        List<String> disabledBiomes = new ArrayList<>(List.of(
                "#minecraft:is_river",
                "minecraft:river",
                "minecraft:frozen_river",
                "minecraft:beach",
                "minecraft:snowy_beach",
                "minecraft:stony_shore"
        ));
    }

    private record DisabledBiomeSelectors(Set<Identifier> ids, Set<TagKey<Biome>> tags) {
    }

    private static DisabledBiomeSelectors parseBiomeSelectors(List<String> values) {
        if (values == null || values.isEmpty()) return new DisabledBiomeSelectors(Set.of(), Set.of());

        HashSet<Identifier> ids = new HashSet<>();
        HashSet<TagKey<Biome>> tags = new HashSet<>();

        for (String raw : values) {
            if (raw == null || raw.isBlank()) continue;
            String s = raw.trim();
            boolean isTag = s.charAt(0) == '#';
            if (isTag) s = s.substring(1);

            try {
                Identifier id = Identifier.parse(s);
                if (isTag) {
                    tags.add(TagKey.create(Registries.BIOME, id));
                } else {
                    ids.add(id);
                }
            } catch (Exception ignored) {
                // Ignore invalid entries.
            }
        }

        return new DisabledBiomeSelectors(Set.copyOf(ids), Set.copyOf(tags));
    }

    private static class ChatHeadsConfig {
        boolean enableChatHeads = true;
        int chatHeadSize = 8;
        int chatHeadOffset = 0;
    }

    private static class KeybindModifiersConfig {
        boolean debugRebindLogging = false;
    }

    private static class UsageTickerConfig {
        boolean enabled = true;
        boolean invert = false;
        int offsetX = 0;
        int offsetY = 0;
        boolean debug = false;
    }

    private static class LeafLitterTintConfig {
        boolean enabled = true;
        String source = "foliage";
        boolean clampEnabled = false;
        String globalMinColor = "";
        String globalMaxColor = "";
        List<String> biomeRanges = new ArrayList<>();
    }
}
