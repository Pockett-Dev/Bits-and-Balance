package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// Client-specific configuration
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Client section
    static {
        BUILDER.comment("Client");
        BUILDER.push("client");
    }

    // Night Vision Improvement subsection
    static {
        BUILDER.comment("Night Vision Improvement");
        BUILDER.push("nightVision");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_NIGHT_VISION_FADE = BUILDER
            .comment("Enable Night Vision fadeout instead of vanilla blinking.")
            .define("enableNightVisionFade", true);
    private static final ModConfigSpec.IntValue NIGHT_VISION_FADE_SECONDS = BUILDER
            .comment("Seconds over which Night Vision should fade out before expiring.")
            .defineInRange("nightVisionFadeSeconds", 10, 1, 30);
    static {
        BUILDER.pop();
    }


    // See Held Item subsection
    static {
        BUILDER.comment("See Held Item");
        BUILDER.push("seeHeldItem");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SHOW_HELD_WHEN_RIDING = BUILDER
            .comment("Show your held item in first-person while riding entities (e.g., boats).")
            .define("enableShowHeldItemWhenRiding", true);
    static {
        BUILDER.pop();
    }

    // Elder Guardian Appearance subsection
    static {
        BUILDER.comment("Elder Guardian Appearance");
        BUILDER.push("guardian");
    }
    private static final ModConfigSpec.BooleanValue DISABLE_GUARDIAN_JUMPSCARE = BUILDER
            .comment("Disable the Elder Guardian appearance effect when Mining Fatigue is inflicted.")
            .define("disableGuardianJumpscare", true);
    private static final ModConfigSpec.BooleanValue PLAY_GUARDIAN_JUMPSCARE_SOUND = BUILDER
            .comment("If true, still plays the normal Elder Guardian Mining Fatigue sound while the appearance effect is suppressed.")
            .define("playGuardianJumpscareSound", true);
    static {
        BUILDER.pop();
    }

    // Auto Walk feature subsection
    static {
        BUILDER.comment("Auto Walk / Controls");
        BUILDER.push("autoWalk");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_AUTO_WALK_KEY = BUILDER
            .comment("Enable the Auto Walk keybind to toggle forward walking (enables Auto Jump while active).")
            .define("enableAutoWalkKey", true);
    static {
        BUILDER.pop();
    }

    // Experience / Sounds subsection
    static {
        BUILDER.comment("Experience / Sounds");
        BUILDER.push("experience");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_LEVEL30_OLD_SOUND = BUILDER
            .comment("Enable the long/old level-up sound when reaching level 30.")
            .define("enableLevel30OldSound", true);
    static {
        BUILDER.pop();
    }

        // Screenshots subsection
        static {
                BUILDER.comment("Screenshots");
                BUILDER.push("screenshots");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_SCREENSHOTS_TO_CLIPBOARD = BUILDER
                        .comment("Copy successful in-game screenshots to the system clipboard and show a client-only confirmation message.")
                        .define("enableScreenshotsToClipboard", true);
        static {
                BUILDER.pop();
        }

    // Custom Splash Text subsection
    static {
        BUILDER.comment("Custom Splash Text");
        BUILDER.push("splashText");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_SPLASH_TEXTS = BUILDER
            .comment("Enable custom splash texts to randomly appear on the title screen.")
            .define("enableCustomSplashTexts", true);
    private static final ModConfigSpec.IntValue CUSTOM_SPLASH_MULTIPLIER = BUILDER
            .comment("How many times to add each custom splash text to increase appearance odds. Higher = more likely to appear.")
            .defineInRange("customSplashMultiplier", 2, 1, 10);
    static {
        BUILDER.pop();
    }

        // Performance subsection
        static {
                BUILDER.comment("Performance");
                BUILDER.push("performance");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_UNCAP_MENU_FPS = BUILDER
                        .comment("Uncap FPS on the title screen and pause/menu screens (uses Minecraft's built-in Unlimited value).")
                        .define("enableUncapMenuFps", true);
        static {
                BUILDER.pop();
        }

        // World / Experimental Warnings subsection
        static {
                BUILDER.comment("World / Experimental Warnings");
                BUILDER.push("worldWarnings");
        }
        private static final ModConfigSpec.BooleanValue SUPPRESS_EXPERIMENTAL_SETTINGS_WARNING = BUILDER
                        .comment("Suppress experimental settings warnings when creating/loading worlds.")
                    .define("suppressExperimentalSettingsWarning", true);
        static {
                BUILDER.pop();
        }

        // Log Filtering subsection
        static {
                BUILDER.comment("Log Filtering");
                BUILDER.push("logFiltering");
        }
        private static final ModConfigSpec.BooleanValue SUPPRESS_FAR_CHUNK_ERRORS = BUILDER
                        .comment("Suppress \"Detected setBlock in a far chunk\" error spam from worldgen features.")
                    .define("suppressFarChunkErrors", true);
        private static final ModConfigSpec.BooleanValue SUPPRESS_REMOVED_RECIPE_BOOK_WARNINGS = BUILDER
                        .comment("Suppress stale recipe-book warnings for recipes that no longer exist in the current world/modpack.")
                    .define("suppressRemovedRecipeBookWarnings", true);
        static {
                BUILDER.pop();
        }

        // Biome Item Tinting subsection
        static {
                BUILDER.comment("Biome Item Tinting");
                BUILDER.push("biomeItemTint");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_BIOME_ITEM_TINT = BUILDER
                        .comment("Tint grass/foliage items in your hand/inventory using the current biome colors.")
                        .define("enabled", true);

        private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIOME_ITEM_TINT_DISABLED_ITEMS = BUILDER
                        .comment(
                                "Block/item IDs that should never be biome-tinted (resource locations).",
                                "Default disables sugar cane."
                        )
                        .defineList("disabledItems", java.util.List.of(
                                "minecraft:sugar_cane"
                        ), ClientConfig::validateIdName);
        static {
                BUILDER.pop();
        }

        // Biome Titles subsection
        static {
                BUILDER.comment("Biome Titles");
                BUILDER.push("biomeTitles");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_BIOME_TITLES = BUILDER
                        .comment("Display a title when entering a new biome (suppressed for recently entered biomes).")
                        .define("enabled", true);
        private static final ModConfigSpec.IntValue BIOME_TITLES_RECENT_SIZE = BUILDER
                        .comment("How many recently entered biomes are remembered (re-entering one will not re-display the title).")
                        .defineInRange("recentSize", 7, 1, 50);
        private static final ModConfigSpec.IntValue BIOME_TITLES_SHOW_SECONDS = BUILDER
                        .comment("Seconds to keep the title fully visible (not counting fade in/out).")
                        .defineInRange("showSeconds", 3, 0, 30);
        private static final ModConfigSpec.IntValue BIOME_TITLES_FADE_SECONDS = BUILDER
                        .comment("Seconds to fade in and fade out.")
                        .defineInRange("fadeSeconds", 1, 0, 10);
        private static final ModConfigSpec.DoubleValue BIOME_TITLES_SCALE = BUILDER
                        .comment("Text scale for the biome title.")
                        .defineInRange("scale", 2.0D, 0.5D, 3.0D);
        private static final ModConfigSpec.ConfigValue<String> BIOME_TITLES_SLOT = BUILDER
                        .comment("Which line to use: 'title' or 'subtitle'.")
                        .define("slot", "subtitle");
        private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BIOME_TITLES_DISABLED_BIOMES = BUILDER
                        .comment(
                                "Biome IDs that should never display a Biome Title (resource locations).",
                                "Biome tags are supported by prefixing with '#'.",
                                "Default disables common tiny transition biomes (rivers/beaches)."
                        )
                        .defineList("disabledBiomes", java.util.List.of(
                                "#minecraft:is_river",
                                "minecraft:river",
                                "minecraft:frozen_river",
                                "minecraft:beach",
                                "minecraft:snowy_beach",
                                "minecraft:stony_shore"
                        ), ClientConfig::validateBiomeSelectorName);
        private static final ModConfigSpec.IntValue BIOME_TITLES_OFFSET_X = BUILDER
                        .comment("Horizontal offset from screen center (pixels).")
                        .defineInRange("offsetX", 0, -400, 400);
        private static final ModConfigSpec.IntValue BIOME_TITLES_OFFSET_Y = BUILDER
                        .comment("Vertical offset from the default title position (pixels).")
                        .defineInRange("offsetY", 0, -400, 400);
        private static final ModConfigSpec.BooleanValue BIOME_TITLES_SHADOW = BUILDER
                        .comment("Render the biome title with a text shadow.")
                        .define("shadow", true);
        static {
                BUILDER.pop();
        }

        // Soul Fire Overlay subsection
        static {
                BUILDER.comment("Soul Fire Overlay");
                BUILDER.push("soulFireOverlay");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_SOUL_FIRE_OVERLAY = BUILDER
                .comment("Render blue soul-fire overlay when burning from soul fire/campfires.")
                .define("enabled", true);
        private static final ModConfigSpec.BooleanValue ENABLE_SOUL_FIRE_CANDLE_FLAMES = BUILDER
                .comment("Use blue soul-fire flame particles for lit candles placed on soul sand or soul soil.")
                .define("candleFlames", true);
        static {
                BUILDER.pop();
        }

        // Leaf Litter / Leaf Pile Tint subsection
        static {
                BUILDER.comment("Leaf Litter / Leaf Pile Tint");
                BUILDER.push("leafLitterTint");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_LEAF_LITTER_TINT = BUILDER
                .comment("Tint leaf litter (minecraft:leaf_litter) and leaf pile blocks using biome colors.")
                .define("enabled", true);
        private static final ModConfigSpec.ConfigValue<String> LEAF_LITTER_TINT_SOURCE = BUILDER
                .comment("Which biome color to use: 'foliage' or 'grass'.")
                .define("source", "foliage");
        private static final ModConfigSpec.BooleanValue LEAF_LITTER_TINT_CLAMP_ENABLED = BUILDER
                .comment("Clamp the resulting tint color into an allowed range (global and/or per-biome).")
                .define("clampEnabled", false);
        private static final ModConfigSpec.ConfigValue<String> LEAF_LITTER_TINT_GLOBAL_MIN = BUILDER
                .comment("Global minimum allowed color as hex (e.g. '#203020'). Empty disables global minimum.")
                .define("globalMinColor", "");
        private static final ModConfigSpec.ConfigValue<String> LEAF_LITTER_TINT_GLOBAL_MAX = BUILDER
                .comment("Global maximum allowed color as hex (e.g. '#80A080'). Empty disables global maximum.")
                .define("globalMaxColor", "");
        private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> LEAF_LITTER_TINT_BIOME_RANGES = BUILDER
                .comment(
                        "Per-biome clamp ranges. Format: <biome>=<minHex>-<maxHex>",
                        "Example: 'minecraft:swamp=#203020-#4E7A4E'",
                        "Biome tags are supported by prefixing with '#'.",
                        "Example tag: '#minecraft:is_overworld=#203020-#80A080'",
                        "If a biome entry is present, it overrides the global clamp for that biome."
                )
                .defineList("biomeRanges", java.util.List.of(), o -> o instanceof String);
        static {
                BUILDER.pop();
        }

    // Chat Heads subsection
    static {
        BUILDER.comment("Chat Heads");
        BUILDER.push("chatHeads");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_CHAT_HEADS = BUILDER
            .comment("Display player skin faces before usernames in chat.")
            .define("enableChatHeads", true);
    private static final ModConfigSpec.IntValue CHAT_HEAD_SIZE = BUILDER
            .comment("Size of the player head icon in pixels.")
            .defineInRange("chatHeadSize", 8, 4, 16);
    private static final ModConfigSpec.IntValue CHAT_HEAD_OFFSET = BUILDER
            .comment("Horizontal offset for the chat head from the message start (in pixels).")
            .defineInRange("chatHeadOffset", 0, -10, 10);
    static {
        BUILDER.pop();
    }

    // Keybind Modifiers subsection
    static {
        BUILDER.comment("Keybind Modifiers - Configure modifier keys (Shift, Alt, Ctrl) for various keybinds");
        BUILDER.push("keybindModifiers");
    }
    private static final ModConfigSpec.BooleanValue ITEM_SHARE_REQUIRE_SHIFT = BUILDER
            .comment("Require Shift to be held when pressing the Item Share keybind.")
            .define("itemShareRequireShift", true);
    private static final ModConfigSpec.BooleanValue ITEM_SHARE_REQUIRE_ALT = BUILDER
            .comment("Require Alt to be held when pressing the Item Share keybind.")
            .define("itemShareRequireAlt", false);
    private static final ModConfigSpec.BooleanValue ITEM_SHARE_REQUIRE_CTRL = BUILDER
            .comment("Require Ctrl to be held when pressing the Item Share keybind.")
            .define("itemShareRequireCtrl", false);
    static {
        BUILDER.pop();
    }


    // Usage Ticker subsection
    static {
        BUILDER.comment("Usage Ticker");
        BUILDER.push("usageTicker");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_USAGE_TICKER = BUILDER
            .comment("Enable the Usage Ticker overlay that shows item counts in your inventory.")
            .define("enabled", true);
        private static final ModConfigSpec.BooleanValue USAGE_TICKER_SHOW_ARMOR_SLOTS = BUILDER
                .comment("Show armor slots in the Usage Ticker overlay. [WIP - Currently buggy]")
                .define("showArmorSlots", false);
    private static final ModConfigSpec.BooleanValue USAGE_TICKER_INVERT = BUILDER
            .comment("Invert the display order of the Usage Ticker overlay.")
            .define("invert", false);
    private static final ModConfigSpec.IntValue USAGE_TICKER_OFFSET_X = BUILDER
            .comment("Horizontal offset for the Usage Ticker overlay (in pixels).")
            .defineInRange("offsetX", 0, -200, 200);
    private static final ModConfigSpec.IntValue USAGE_TICKER_OFFSET_Y = BUILDER
            .comment("Vertical offset for the Usage Ticker overlay (in pixels).")
            .defineInRange("offsetY", 0, -200, 200);
    private static final ModConfigSpec.IntValue USAGE_TICKER_ARMOR_OFFSET_X = BUILDER
            .comment("Horizontal offset for armor slots in the Usage Ticker overlay (in pixels).")
            .defineInRange("armorOffsetX", 0, -200, 200);
    private static final ModConfigSpec.IntValue USAGE_TICKER_ARMOR_OFFSET_Y = BUILDER
            .comment("Vertical offset for armor slots in the Usage Ticker overlay (in pixels).")
            .defineInRange("armorOffsetY", 0, -200, 200);
    private static final ModConfigSpec.BooleanValue USAGE_TICKER_DEBUG = BUILDER
            .comment("Enable debug messages for the Usage Ticker (useful for troubleshooting).")
            .define("debug", false);
    
    // Armor Animation sub-configuration
    static {
        BUILDER.comment("Armor Animation Settings");
        BUILDER.push("armorAnimation");
    }
    
    // Helmet animation settings
    private static final ModConfigSpec.IntValue ARMOR_HELMET_START_Y = BUILDER
            .comment("Starting Y position for helmet animation (pixels below screen bottom).")
            .defineInRange("helmetStartY", 50, 0, 200);
    private static final ModConfigSpec.IntValue ARMOR_HELMET_END_Y = BUILDER
            .comment("Ending Y position for helmet animation (pixels above screen bottom).")
            .defineInRange("helmetEndY", 24, 0, 200);
    
    // Chestplate animation settings
    private static final ModConfigSpec.IntValue ARMOR_CHESTPLATE_START_Y = BUILDER
            .comment("Starting Y position for chestplate animation (pixels below screen bottom).")
            .defineInRange("chestplateStartY", 50, 0, 200);
    private static final ModConfigSpec.IntValue ARMOR_CHESTPLATE_END_Y = BUILDER
            .comment("Ending Y position for chestplate animation (pixels above screen bottom).")
            .defineInRange("chestplateEndY", 24, 0, 200);
    
    // Leggings animation settings
    private static final ModConfigSpec.IntValue ARMOR_LEGGINGS_START_Y = BUILDER
            .comment("Starting Y position for leggings animation (pixels below screen bottom).")
            .defineInRange("leggingsStartY", 50, 0, 200);
    private static final ModConfigSpec.IntValue ARMOR_LEGGINGS_END_Y = BUILDER
            .comment("Ending Y position for leggings animation (pixels above screen bottom).")
            .defineInRange("leggingsEndY", 24, 0, 200);
    
    // Boots animation settings
    private static final ModConfigSpec.IntValue ARMOR_BOOTS_START_Y = BUILDER
            .comment("Starting Y position for boots animation (pixels below screen bottom).")
            .defineInRange("bootsStartY", 50, 0, 200);
    private static final ModConfigSpec.IntValue ARMOR_BOOTS_END_Y = BUILDER
            .comment("Ending Y position for boots animation (pixels above screen bottom).")
            .defineInRange("bootsEndY", 24, 0, 200);
    
    
    static {
        BUILDER.pop(); // armorAnimation
        BUILDER.pop(); // usageTicker
        BUILDER.pop(); // client
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Client flag values (loaded from config)
    public static boolean enableNightVisionFade;
    public static int nightVisionFadeSeconds;
    public static boolean enableShowHeldItemWhenRiding;
    public static boolean disableGuardianJumpscare;
        public static boolean playGuardianJumpscareSound;
    public static boolean enableAutoWalkKey;
    public static boolean enableLevel30OldSound;
        public static boolean enableScreenshotsToClipboard;
    public static boolean enableCustomSplashTexts;
    public static int customSplashMultiplier;

        public static boolean enableUncapMenuFps;

        // World / Experimental Warnings values
        public static boolean suppressExperimentalSettingsWarning;

    // Log Filtering values
    public static boolean suppressFarChunkErrors;
        public static boolean suppressRemovedRecipeBookWarnings;

        // Biome Item Tinting values
        public static boolean biomeItemTintEnabled;
        public static java.util.List<String> biomeItemTintDisabledItems = java.util.List.of("minecraft:sugar_cane");
        public static java.util.Set<net.minecraft.resources.Identifier> biomeItemTintDisabledItemIds = java.util.Set.of(net.minecraft.resources.Identifier.parse("minecraft:sugar_cane"));
    
    // Chat Heads values
    public static boolean enableChatHeads;
    public static int chatHeadSize;
    public static int chatHeadOffset;
    
    // Keybind Modifiers values
    public static boolean itemShareRequireShift;
    public static boolean itemShareRequireAlt;
    public static boolean itemShareRequireCtrl;
    
    // Usage Ticker values
    public static boolean usageTickerEnabled;
    public static boolean usageTickerShowArmorSlots;
    public static boolean usageTickerInvert;
    public static int usageTickerOffsetX;
    public static int usageTickerOffsetY;
    public static int usageTickerArmorOffsetX;
    public static int usageTickerArmorOffsetY;
    public static boolean usageTickerDebug;
    
    // Armor Animation values
    public static int armorHelmetStartY;
    public static int armorHelmetEndY;
    public static int armorChestplateStartY;
    public static int armorChestplateEndY;
    public static int armorLeggingsStartY;
    public static int armorLeggingsEndY;
    public static int armorBootsStartY;
    public static int armorBootsEndY;

	// Leaf Litter / Leaf Pile Tint values
	public static boolean leafLitterTintEnabled;
	public static String leafLitterTintSource;
	public static boolean leafLitterTintClampEnabled;
	public static String leafLitterTintGlobalMinColor;
	public static String leafLitterTintGlobalMaxColor;
	public static int leafLitterTintGlobalMinColorRgb;
	public static int leafLitterTintGlobalMaxColorRgb;
        public static java.util.Map<net.minecraft.resources.Identifier, int[]> leafLitterTintBiomeRanges = java.util.Map.of();
        public static java.util.List<BiomeTagRange> leafLitterTintBiomeTagRanges = java.util.List.of();
    
    

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        // Load client config values
        enableNightVisionFade = ENABLE_NIGHT_VISION_FADE.get();
        nightVisionFadeSeconds = NIGHT_VISION_FADE_SECONDS.get();
        enableShowHeldItemWhenRiding = ENABLE_SHOW_HELD_WHEN_RIDING.get();
        disableGuardianJumpscare = DISABLE_GUARDIAN_JUMPSCARE.get();
        playGuardianJumpscareSound = PLAY_GUARDIAN_JUMPSCARE_SOUND.get();
        enableAutoWalkKey = ENABLE_AUTO_WALK_KEY.get();
        enableLevel30OldSound = ENABLE_LEVEL30_OLD_SOUND.get();
        enableScreenshotsToClipboard = ENABLE_SCREENSHOTS_TO_CLIPBOARD.get();
        enableCustomSplashTexts = ENABLE_CUSTOM_SPLASH_TEXTS.get();
        customSplashMultiplier = CUSTOM_SPLASH_MULTIPLIER.get();
        enableUncapMenuFps = ENABLE_UNCAP_MENU_FPS.get();
        suppressExperimentalSettingsWarning = SUPPRESS_EXPERIMENTAL_SETTINGS_WARNING.get();
        suppressFarChunkErrors = SUPPRESS_FAR_CHUNK_ERRORS.get();
        suppressRemovedRecipeBookWarnings = SUPPRESS_REMOVED_RECIPE_BOOK_WARNINGS.get();
        biomeItemTintEnabled = ENABLE_BIOME_ITEM_TINT.get();
        biomeItemTintDisabledItems = new java.util.ArrayList<>(BIOME_ITEM_TINT_DISABLED_ITEMS.get());
        biomeItemTintDisabledItemIds = parseIds(biomeItemTintDisabledItems);

        // Biome titles
        int showTicks = BIOME_TITLES_SHOW_SECONDS.get() * 20;
        int fadeTicks = BIOME_TITLES_FADE_SECONDS.get() * 20;

        Config.biomeTitlesEnabled = ENABLE_BIOME_TITLES.get();
        Config.biomeTitlesRecentSize = BIOME_TITLES_RECENT_SIZE.get();
        Config.biomeTitlesShowTicks = showTicks;
        Config.biomeTitlesFadeTicks = fadeTicks;
        Config.biomeTitlesScale = BIOME_TITLES_SCALE.get();
        Config.biomeTitlesSlot = BIOME_TITLES_SLOT.get();
        Config.biomeTitlesOffsetX = BIOME_TITLES_OFFSET_X.get();
        Config.biomeTitlesOffsetY = BIOME_TITLES_OFFSET_Y.get();
        Config.biomeTitlesShadow = BIOME_TITLES_SHADOW.get();
        DisabledBiomeSelectors selectors = parseBiomeSelectors(BIOME_TITLES_DISABLED_BIOMES.get());
        Config.biomeTitlesDisabledBiomeIds = selectors.ids();
        Config.biomeTitlesDisabledBiomeTags = selectors.tags();

        // Soul fire overlay
        Config.soulFireOverlayEnabled = ENABLE_SOUL_FIRE_OVERLAY.get();
        Config.soulFireCandleFlamesEnabled = ENABLE_SOUL_FIRE_CANDLE_FLAMES.get();

        // Apply log filter settings
        org.onenonly.bitsandbalance.common.client.FarChunkErrorFilter.setEnabled(suppressFarChunkErrors);

        // Leaf litter tint
        leafLitterTintEnabled = ENABLE_LEAF_LITTER_TINT.get();
        leafLitterTintSource = LEAF_LITTER_TINT_SOURCE.get();
        leafLitterTintClampEnabled = LEAF_LITTER_TINT_CLAMP_ENABLED.get();
        leafLitterTintGlobalMinColor = LEAF_LITTER_TINT_GLOBAL_MIN.get();
        leafLitterTintGlobalMaxColor = LEAF_LITTER_TINT_GLOBAL_MAX.get();
		leafLitterTintGlobalMinColorRgb = tryParseHexColor(leafLitterTintGlobalMinColor);
		leafLitterTintGlobalMaxColorRgb = tryParseHexColor(leafLitterTintGlobalMaxColor);
                var parsed = parseBiomeRanges(LEAF_LITTER_TINT_BIOME_RANGES.get());
                leafLitterTintBiomeRanges = parsed.byBiome();
                leafLitterTintBiomeTagRanges = parsed.byTag();
        
        // Load Chat Heads values
        enableChatHeads = ENABLE_CHAT_HEADS.get();
        chatHeadSize = CHAT_HEAD_SIZE.get();
        chatHeadOffset = CHAT_HEAD_OFFSET.get();
        
        // Load Keybind Modifiers values
        itemShareRequireShift = ITEM_SHARE_REQUIRE_SHIFT.get();
        itemShareRequireAlt = ITEM_SHARE_REQUIRE_ALT.get();
        itemShareRequireCtrl = ITEM_SHARE_REQUIRE_CTRL.get();
        
        // Load Usage Ticker values
        usageTickerEnabled = ENABLE_USAGE_TICKER.get();
        usageTickerShowArmorSlots = USAGE_TICKER_SHOW_ARMOR_SLOTS.get();
        usageTickerInvert = USAGE_TICKER_INVERT.get();
        usageTickerOffsetX = USAGE_TICKER_OFFSET_X.get();
        usageTickerOffsetY = USAGE_TICKER_OFFSET_Y.get();
        usageTickerArmorOffsetX = USAGE_TICKER_ARMOR_OFFSET_X.get();
        usageTickerArmorOffsetY = USAGE_TICKER_ARMOR_OFFSET_Y.get();
        usageTickerDebug = USAGE_TICKER_DEBUG.get();
        
        // Load Armor Animation values
        armorHelmetStartY = ARMOR_HELMET_START_Y.get();
        armorHelmetEndY = ARMOR_HELMET_END_Y.get();
        armorChestplateStartY = ARMOR_CHESTPLATE_START_Y.get();
        armorChestplateEndY = ARMOR_CHESTPLATE_END_Y.get();
        armorLeggingsStartY = ARMOR_LEGGINGS_START_Y.get();
        armorLeggingsEndY = ARMOR_LEGGINGS_END_Y.get();
        armorBootsStartY = ARMOR_BOOTS_START_Y.get();
        armorBootsEndY = ARMOR_BOOTS_END_Y.get();
        
        

        // Update main Config class with client values
        Config.enableNightVisionFade = enableNightVisionFade;
        Config.nightVisionFadeSeconds = nightVisionFadeSeconds;
        Config.disableGuardianJumpscare = disableGuardianJumpscare;
        Config.playGuardianJumpscareSound = playGuardianJumpscareSound;
        Config.enableAutoWalkKey = enableAutoWalkKey;
        Config.enableShowHeldItemWhenRiding = enableShowHeldItemWhenRiding;
        Config.enableLevel30OldSound = enableLevel30OldSound;
        Config.enableCustomSplashTexts = enableCustomSplashTexts;
        Config.enableUncapMenuFps = enableUncapMenuFps;
        
        // Update main Config class with Chat Heads values
        Config.enableChatHeads = enableChatHeads;
        Config.chatHeadSize = chatHeadSize;
        Config.chatHeadOffset = chatHeadOffset;
        
        // Update main Config class with Usage Ticker values
        Config.usageTickerEnabled = usageTickerEnabled;
        Config.usageTickerShowArmorSlots = usageTickerShowArmorSlots;
        Config.usageTickerInvert = usageTickerInvert;
        Config.usageTickerOffsetX = usageTickerOffsetX;
        Config.usageTickerOffsetY = usageTickerOffsetY;
        Config.usageTickerArmorOffsetX = usageTickerArmorOffsetX;
        Config.usageTickerArmorOffsetY = usageTickerArmorOffsetY;
        Config.usageTickerDebug = usageTickerDebug;
        
        // Update main Config class with Armor Animation values
        Config.armorHelmetStartY = armorHelmetStartY;
        Config.armorHelmetEndY = armorHelmetEndY;
        Config.armorChestplateStartY = armorChestplateStartY;
        Config.armorChestplateEndY = armorChestplateEndY;
        Config.armorLeggingsStartY = armorLeggingsStartY;
        Config.armorLeggingsEndY = armorLeggingsEndY;
        Config.armorBootsStartY = armorBootsStartY;
        Config.armorBootsEndY = armorBootsEndY;
        
        
        // Note: enableSplashRefreshKey is only used in ClientConfig, not in main Config
    }

        private static boolean validateBiomeSelectorName(final Object obj) {
                if (!(obj instanceof String s)) return false;
                s = s.trim();
                if (s.isEmpty()) return false;
                if (s.charAt(0) == '#') {
                        s = s.substring(1);
                }
                try {
                                        net.minecraft.resources.Identifier.parse(s);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private static boolean validateIdName(final Object obj) {
                if (!(obj instanceof String s)) return false;
                s = s.trim();
                if (s.isEmpty()) return false;
                try {
                        net.minecraft.resources.Identifier.parse(s);
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private static java.util.Set<net.minecraft.resources.Identifier> parseIds(java.util.List<? extends String> values) {
                if (values == null || values.isEmpty()) return java.util.Set.of();

                java.util.HashSet<net.minecraft.resources.Identifier> ids = new java.util.HashSet<>();
                for (String raw : values) {
                        if (raw == null || raw.isBlank()) continue;
                        try {
                                ids.add(net.minecraft.resources.Identifier.parse(raw.trim()));
                        } catch (Exception ignored) {
                                // Ignore invalid entries.
                        }
                }
                return java.util.Set.copyOf(ids);
        }

        private record DisabledBiomeSelectors(
                                                java.util.Set<net.minecraft.resources.Identifier> ids,
                        java.util.Set<net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome>> tags
        ) {
        }

        private static DisabledBiomeSelectors parseBiomeSelectors(java.util.List<? extends String> values) {
                if (values == null || values.isEmpty()) return new DisabledBiomeSelectors(java.util.Set.of(), java.util.Set.of());

                                java.util.HashSet<net.minecraft.resources.Identifier> ids = new java.util.HashSet<>();
                java.util.HashSet<net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome>> tags = new java.util.HashSet<>();

                for (String raw : values) {
                        if (raw == null || raw.isBlank()) continue;
                        String s = raw.trim();
                        boolean isTag = s.charAt(0) == '#';
                        if (isTag) s = s.substring(1);

                        try {
                                                        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(s);
                                if (isTag) {
                                        tags.add(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, id));
                                } else {
                                        ids.add(id);
                                }
                        } catch (Exception ignored) {
                                // Ignore invalid entries.
                        }
                }

                return new DisabledBiomeSelectors(java.util.Set.copyOf(ids), java.util.Set.copyOf(tags));
        }

        public record BiomeTagRange(net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag, int[] range) {}
        private record ParsedBiomeRanges(
                                java.util.Map<net.minecraft.resources.Identifier, int[]> byBiome,
                java.util.List<BiomeTagRange> byTag
        ) {}

        private static ParsedBiomeRanges parseBiomeRanges(java.util.List<? extends String> entries) {
                if (entries == null || entries.isEmpty()) {
                        return new ParsedBiomeRanges(java.util.Map.of(), java.util.List.of());
                }

                                java.util.Map<net.minecraft.resources.Identifier, int[]> byBiome = new java.util.HashMap<>();
                java.util.List<BiomeTagRange> byTag = new java.util.ArrayList<>();
                for (String raw : entries) {
                        if (raw == null) {
                                continue;
                        }
                        String s = raw.trim();
                        if (s.isEmpty()) {
                                continue;
                        }

                        int eq = s.indexOf('=');
                        if (eq <= 0 || eq >= s.length() - 1) {
                                continue;
                        }

                        String key = s.substring(0, eq).trim();
                        String range = s.substring(eq + 1).trim();
                        int dash = range.indexOf('-');
                        if (dash <= 0 || dash >= range.length() - 1) {
                                continue;
                        }

                        int min = tryParseHexColor(range.substring(0, dash).trim());
                        int max = tryParseHexColor(range.substring(dash + 1).trim());
                        if (min == -1 || max == -1) {
                                continue;
                        }

                        if (key.startsWith("#")) {
                                String tagId = key.substring(1).trim();
                                if (tagId.isEmpty()) {
                                        continue;
                                }
                                                net.minecraft.resources.Identifier tagLoc = net.minecraft.resources.Identifier.tryParse(tagId);
                                if (tagLoc == null) {
                                        continue;
                                }
                                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME, tagLoc);
                                byTag.add(new BiomeTagRange(tag, new int[] { min, max }));
                                } else {
                                                net.minecraft.resources.Identifier biome = net.minecraft.resources.Identifier.tryParse(key);
                                if (biome == null) {
                                        continue;
                                }
                                byBiome.put(biome, new int[] { min, max });
                        }
                }

                return new ParsedBiomeRanges(java.util.Map.copyOf(byBiome), java.util.List.copyOf(byTag));
        }

	private static int tryParseHexColor(String raw) {
		if (raw == null) {
			return -1;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return -1;
		}

		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		if (s.startsWith("0x") || s.startsWith("0X")) {
			s = s.substring(2);
		}
		if (s.length() != 6) {
			return -1;
		}

		try {
			return Integer.parseInt(s, 16) & 0xFFFFFF;
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}
}
