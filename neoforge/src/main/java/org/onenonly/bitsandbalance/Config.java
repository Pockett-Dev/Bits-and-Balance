package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;

/**
 * Centralized configuration for Bits and Balance mod
 * Organized by feature categories for easy maintenance
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========================================
    // COMBAT FEATURES
    // ========================================
    static {
        BUILDER.comment("Combat Features");
        BUILDER.push("combat");
    }

    // Second Chance
    static { BUILDER.comment("Second Chance"); BUILDER.push("secondChance"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SECOND_CHANCE = BUILDER
            .comment("Enable the Second Chance anti-one-shot mechanic for players (excludes fall damage by default).")
            .define("enableSecondChance", true);
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_EXCLUDE_FALL = BUILDER
            .comment("Exclude fall damage from triggering Second Chance.")
            .define("excludeFallDamage", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_COOLDOWN_DAYS = BUILDER
            .comment("Cooldown in Minecraft days between Second Chance triggers (1 day = 24000 ticks). Set to 0 to disable cooldown.")
            .defineInRange("cooldownDays", 7, 0, 3650);
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_RESET_ON_RESPAWN = BUILDER
            .comment("Reset Second Chance cooldown on respawn (death). If false, cooldown persists across deaths.")
            .define("resetOnRespawn", true);
    
    static { BUILDER.comment("Apply Resistance effect when Second Chance triggers."); BUILDER.push("applyResistance"); }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_RESISTANCE_ENABLED = BUILDER
            .comment("Enable applying Resistance when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_RESISTANCE_LEVEL = BUILDER
            .comment("Resistance effect level to apply (1 = Resistance I).")
            .defineInRange("level", 1, 1, 10);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Apply Nausea effect when Second Chance triggers."); BUILDER.push("applyNausea"); }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_NAUSEA_ENABLED = BUILDER
            .comment("Enable applying Nausea when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_NAUSEA_LEVEL = BUILDER
            .comment("Nausea effect level to apply (1 = Nausea I).")
            .defineInRange("level", 2, 1, 10);
    static { BUILDER.pop(); BUILDER.pop(); }

    // Maintain Experience
    static { BUILDER.comment("Maintain Experience"); BUILDER.push("maintainExperience"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MAINTAIN_EXPERIENCE = BUILDER
            .comment("Enable Maintain Experience: players lose a percentage of their total XP on death, not a percentage of displayed levels.")
            .define("enableMaintainExperience", true);
    private static final ModConfigSpec.IntValue MAINTAIN_EXPERIENCE_LOSS_PERCENT = BUILDER
            .comment("Percentage of total XP points to lose on death (0-100), not displayed levels.")
            .defineInRange("lossPercent", 50, 0, 100);
    static { BUILDER.pop(); }

    // Snowball Rework
    static { BUILDER.comment("Snowball Rework"); BUILDER.push("snowball"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SNOWBALL_REWORK = BUILDER
            .comment("Enable Snowball Rework: snowballs deal damage and apply freezing.")
            .define("enableSnowballRework", true);
    private static final ModConfigSpec.DoubleValue SNOWBALL_BASE_DAMAGE = BUILDER
            .comment("Base damage dealt by snowballs to entities.")
            .defineInRange("snowballBaseDamage", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue SNOWBALL_NETHER_DAMAGE = BUILDER
            .comment("Damage dealt by snowballs to entities considered Nether-native.")
            .defineInRange("snowballNetherDamage", 1.0D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue SNOWBALL_MIN_FREEZE_SECONDS = BUILDER
            .comment("Minimum freeze duration in seconds applied by snowballs.")
            .defineInRange("snowballMinFreezeSeconds", 5, 0, 60);
    private static final ModConfigSpec.IntValue SNOWBALL_MAX_FREEZE_SECONDS = BUILDER
            .comment("Maximum freeze duration in seconds applied by snowballs.")
            .defineInRange("snowballMaxFreezeSeconds", 8, 0, 60);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SNOWBALL_NETHER_ENTITIES = BUILDER
            .comment("Entity types treated as Nether-native for snowball damage (resource locations).")
            .defineList("snowballNetherEntities", List.of(
                    "minecraft:blaze", "minecraft:ghast", "minecraft:magma_cube", "minecraft:strider",
                    "minecraft:wither_skeleton", "minecraft:piglin", "minecraft:piglin_brute",
                    "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin"
            ), Config::validateEntityTypeName);
    static { BUILDER.pop(); BUILDER.pop(); }

    // ========================================
    // BUILD CONFIG SPEC
    // ========================================
    public static final ModConfigSpec SPEC = BUILDER.build();

    // ========================================
    // STATIC FIELDS (LOADED FROM CONFIG)
    // ========================================
    
    // Combat flags
    public static boolean enableSecondChance;
    public static boolean secondChanceResistanceEnabled;
    public static int secondChanceResistanceLevel;
    public static boolean secondChanceNauseaEnabled;
    public static int secondChanceNauseaLevel;
    public static boolean secondChanceExcludeFall;
    public static int secondChanceCooldownDays;
    public static boolean secondChanceResetOnRespawn;
    public static boolean enableMaintainExperience;
    public static int maintainExperienceLossPercent;
    public static boolean enableSnowballRework;
    public static double snowballBaseDamage;
    public static double snowballNetherDamage;
    public static int snowballMinFreezeSeconds;
    public static int snowballMaxFreezeSeconds;
    public static Set<EntityType<?>> snowballNetherEntityTypes;

    // Potions flags
    public static boolean enableWitheringPotion;
    public static boolean enableResurfacingPotion;
    public static boolean enableDisplacementPotion;
    public static boolean enableReturningPotion;
    public static boolean enableLevitationPotion;
    public static boolean enableHastePotion;
    public static boolean enableGlowingPotion;
    public static boolean enableBioluminescencePotion;

    // Mechanics flags
    public static boolean enableToggleStance = true; // Default to true to match config default
    public static boolean enableCrawlingMechanic = true; // Default to true to match config default
        public static boolean enableFriendlyFireFriendlies = true; // Default to true to match config default
    public static boolean enableItemSharing;
        public static double itemShareCooldownSeconds = 1.0D;
    public static boolean enableCampfiresIgniteEntities;
    public static boolean enableShowHeldItemWhenRiding;
    public static boolean enableDismountEntities;
    public static boolean toggleSitting;
    public static boolean allowDismountPlayers;
    public static double dismountVerticalVelocity;
    public static double dismountHorizontalVelocity;
    public static boolean enableVillagersFollowEmeralds;
    public static double villagerEmeraldFollowSpeed;
    public static boolean villagerEmeraldShowParticles;
    public static int villagerEmeraldParticlesCount;
    public static boolean enableDoorKnocking;
    public static boolean enableCoyoteTimeJump;
        public static int coyoteTimeDelayMs;
        public static int coyoteTimeWindowMs;
    public static boolean coyoteTimeDebug;
    public static boolean enableRapidFireJump;
    public static int rapidFireJumpInterval;
    public static boolean rapidFireJumpDebug;
    public static boolean enableQuickHarvesting;
    public static boolean enableQuickHarvestingHoes;
    public static boolean enableQuickHarvestingAxes;
    public static boolean enableQuickHarvestingHomeDropsToUser;
    public static boolean enableCustomRecoveryCompassRecipe;

        // Tweaks > Improved Recovery Compass
        public static boolean enableImprovedRecoveryCompass = true;
        public static boolean recoveryCompassKeepOnDeath = true;
        public static boolean recoveryCompassShowDistance = true;
        public static boolean recoveryCompassShowCoordsOnRightClick = true;
        public static boolean recoveryCompassSculkParticles = true;

        // Tweaks > Automatic Tool Restock
        public static boolean enableAutomaticToolRestock = true;

        // Tweaks > Automatic Block Restock
        public static boolean enableAutomaticBlockRestock = true;
    
    // Mechanics > Cozy Campfire
    public static boolean enableCozyCampfire;
    public static double cozyCampfireRange;
    public static int cozyCampfireDurationTicks;
    public static int cozyCampfireIntervalTicks;
    public static int cozyCampfireAmplifier;
    public static boolean cozyCampfireAffectBees;
    
    // Mechanics > Speedy Wolves
    public static boolean enableSpeedyWolves;
    public static double speedyWolvesSpeedMultiplier;
    public static double speedyWolvesDistanceThreshold;
    public static double speedyWolvesStepHeight;
    
    // Mechanics > Leashed Teleport
    public static boolean enableLeashedTeleport;
    public static boolean leashedTeleportAllowCrossDimension;
    public static int leashedTeleportMaxFollowers;
    public static double leashedTeleportScanRadius;
    public static int leashedTeleportSafePlacementTries;
    public static double leashedTeleportBaseRadius;
    public static double leashedTeleportMaxRadius;
    public static boolean leashedTeleportPostTeleportLeash;
    public static int leashedTeleportCooldownTicks;
    public static Set<EntityType<?>> leashedTeleportBlacklistTypes;
    
    // Mechanics > Chat Mentions
    public static boolean enableChatMentions;
    public static java.util.List<String> mentionTriggerModes;
    public static boolean mentionCaseInsensitive;
    public static boolean mentionWordBoundary;
    public static boolean mentionRecipientOnlyHighlight;
    public static int mentionMaxMentionsPerMessage;
    public static int mentionCooldownMsPerSender;
    public static boolean mentionAllowSelfPing;
    public static boolean mentionTabCompleteUsernames;
    public static boolean mentionInputHighlightUsernames;
    public static String mentionHighlightColorHex;
    public static String mentionHoverText;
    public static String mentionSoundEvent;
    public static String mentionSoundSource;
    public static double mentionSoundVolume;
    public static double mentionSoundPitch;
    
    // Mechanics > Navigator Compass
    public static boolean enableNavigatorCompass;
    public static boolean navigatorCompassShowDistance;
    public static boolean navigatorCompassShowCoordinates;

    // Mechanics > Bottle of Cloud
    public static boolean enableBottleOfCloud = true;
    public static int bottleOfCloudDurationTicks = 400;
    public static int bottleOfCloudFadeStartTicks = 100;
    public static double bottleOfCloudPlacementDistance = 2.0D;
    public static double bottleOfCloudFallDamageMultiplier = 0.5D;
    public static boolean enableGlowGoo = true;
    public static boolean glowGooTwoGlowInkRecipeEnabled = true;
    public static boolean glowGooFourGlowInkRecipeEnabled = true;
    public static boolean glowGooImpactParticlesEnabled = true;
    public static int glowGooImpactParticleCount = 16;
    public static boolean glowGooAmbientSplatterParticlesEnabled = true;
    public static boolean glowGooBioluminescenceEnabled = true;
    public static int glowGooBioluminescenceDurationSeconds = 60;
    public static int glowGooBioluminescenceLightLevel = 14;
    
    // Wolf Step Height
    public static boolean enableWolfStepHeight;
    public static double wolfStepHeightValue;

    // Client flags
    public static boolean enableNightVisionFade;
    public static int nightVisionFadeSeconds;
    public static boolean disableGuardianJumpscare;
    public static boolean playGuardianJumpscareSound = true;
    public static boolean enableUncapMenuFps;
    public static boolean enableAutoWalkKey;
    public static boolean enableLevel30OldSound = true;
    public static double consumeAnimationSpeedMultiplier = 1.0D;
    public static boolean enableCustomSplashTexts;
    
    // Client > Chat Heads
    public static boolean enableChatHeads = true;
    public static int chatHeadSize = 8;
    public static int chatHeadOffset = 0;

    // Client > Usage Ticker (minimal fields for compile; can be wired to spec later)
    public static boolean usageTickerEnabled = true;
    public static boolean usageTickerInvert = false;
    public static int usageTickerOffsetX = 0;
    public static int usageTickerOffsetY = 0;
    public static boolean usageTickerDebug = false;

    // Client > Biome Titles
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
    /** Biomes that should never display a title (resource locations). */
    public static Set<Identifier> biomeTitlesDisabledBiomeIds = Set.of(
                Identifier.parse("minecraft:river"),
                Identifier.parse("minecraft:frozen_river"),
                Identifier.parse("minecraft:beach"),
                Identifier.parse("minecraft:snowy_beach"),
                Identifier.parse("minecraft:stony_shore")
        );
    /** Biome tags that should never display a title (tag IDs). */
    public static Set<TagKey<Biome>> biomeTitlesDisabledBiomeTags = Set.of(
            TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_river"))
    );

    // Client > Soul Fire Overlay
    public static boolean soulFireOverlayEnabled = true;
    public static boolean soulFireCandleFlamesEnabled = true;
    
    // Tweaks flags
    public static boolean enableArmedArmorStands;
    public static boolean enableDoubleDoorOpening;
    public static boolean doubleDoorCrouchSingle;
    public static boolean doubleDoorChainTrapdoors;
    public static boolean doubleDoorSameBlockOnly;
    public static boolean doubleDoorsWithRedstone;
    public static boolean doubleDoorRedstoneIncludeIron;
    public static boolean doubleDoorRedstoneIncludeTrapdoors;
    public static boolean enableClimbableChainPlacement;
    public static boolean enableFastClimbableSlide;
    public static double fastClimbableSlideSpeed;
    public static int fastClimbableLookDownMinDeg;
    public static int fastClimbableLookDownMaxDeg;
    public static boolean enableFastClimbableAscend;
    public static double fastClimbableAscendSpeed;
    public static int fastClimbableLookUpMinDeg;
    public static int fastClimbableLookUpMaxDeg;

    // Tweaks > Despawn With Master
    public static boolean despawnVexWithEvoker;
    public static boolean despawnBulletsWithShulker;

    // Tweaks > Creeper Sunlight Burn
    public static boolean enableCreeperSunlightBurn;

    // Tweaks > Sophisticated Scaffolding
    public static boolean enableSophisticatedScaffolding;

    // Tweaks > Improved Recovery Compass
    public static boolean enableCompostableItems;
                // Nerfed Mending flags
                public static boolean enableNerfedMending = true;
        /**
         * How many times more XP Mending should require for the same repair.
         * Higher = worse (slower repairs). Intended range: 2-10.
         */
        public static int nerfedMendingMultiplier = 4;
    public static boolean enableCompostableRottenFlesh;
    public static boolean enableCompostablePoisonousPotato;

    // Tweaks > Responsive Shields
    public static boolean enableResponsiveShields;
    public static int shieldRaiseTime;

    // Tweaks > Sugarcane Sand
    public static boolean enableSugarcaneSand;

    // Tweaks > More Mining XP
    public static boolean enableMoreMiningXp;
    public static int coalXpMin;
    public static int coalXpMax;
    public static int diamondXpMin;
    public static int diamondXpMax;
    public static int emeraldXpMin;
    public static int emeraldXpMax;
    public static int lapisXpMin;
    public static int lapisXpMax;
    public static int redstoneXpMin;
    public static int redstoneXpMax;

    // Tweaks > Glowing Glowberries
    public static boolean enableGlowingGlowberries;
    public static int glowingGlowberriesDuration;
    public static int foxGlowingDuration;

    // Tweaks > Enderdragon Egg Always
    public static boolean enableEnderdragonEggAlways;
    public static boolean enderdragonEggUseRandomPlacement;
    public static int enderdragonEggSpawnRadius;
    public static int enderdragonEggSpawnDelay;
    public static boolean enderdragonEggShowParticles;

    // World > Azalea Woodset
    public static boolean enableAzaleaWoodset = true;
    public static boolean enableAzaleaWoodGeneration = true;
    public static boolean enableDogMusicDisc = true;

    // World > Ore Variants
    public static boolean enableOreVariants = true;
    public static boolean enableParallelWorldgenProcessing = false;
    public static int parallelWorldgenThreadCount = 0;

    // World > Veins (built-in datapack worldgen)
    public static boolean enableNetherGoldVeins = true;
    public static boolean enableNetherQuartzVeins = true;
        public static boolean enableRawQuartzBlockInQuartzVeins = true;
    // World > Raw Quartz Block
    public static boolean enableRawQuartzBlock = true;
    public static boolean enableCoalAndesiteVeins = true;
        public static boolean enableCoalTuffVeins = true;

        // World > Veins (debug)
        public static boolean debugVeins = false;

    // World > Nether
    public static boolean disableNetherLavaSprings = false;

        public static boolean keepExposedNetherLavaSprings = true;

    // Tweaks > Brushing XP Rewards
    public static boolean brushingXpEnabled;
    public static int brushingXpMin;
    public static int brushingXpMax;
    
    // Tweaks > Disable Nitwits
    public static boolean disableNitwits;

    // Tweaks > Curse Uses
    public static boolean curseHidePumpkinOverlayOnVanishing;

    // Tweaks > Unbreakable Trial Spawners
    public static boolean enableUnbreakableTrialSpawners;

        // Tweaks > Unbreakable Vaults
        public static boolean enableUnbreakableVaults;

    // Tweaks > Treasure Enchantment Gold Color
    public static boolean enableTreasureEnchantmentGoldColor;

    // Balance flags
    public static boolean enableFullEnderDragonXp;
    public static boolean enableFoodAlwaysEdible;
    public static boolean enableSourceDependentIFrames;
        public static Set<Identifier> sourceIFrameBlacklist;

    // Balance > Depth Scaling Enemies (public fields)
    public static boolean dseEnabled;
    public static boolean dseOverworldOnly;
        public static boolean dseEnableSurfaceTier;
        public static boolean dseEnableMidTier;
        public static boolean dseEnableDeepTier;
    public static int dseMidDepthY;
    public static int dseDeepDepthY;
    public static double dseSurfaceEquipChance;
    public static double dseMidEquipChance;
    public static double dseDeepEquipChance;
    public static double dseSurfaceLeatherWeight;
    public static double dseSurfaceChainWeight;
    public static double dseSurfaceCopperWeight;
    public static double dseSurfaceIronWeight;
    public static double dseSurfaceGoldWeight;
    public static double dseMidLeatherWeight;
    public static double dseMidChainWeight;
    public static double dseMidCopperWeight;
    public static double dseMidIronWeight;
    public static double dseMidGoldWeight;
    public static double dseDeepLeatherWeight;
    public static double dseDeepChainWeight;
    public static double dseDeepCopperWeight;
    public static double dseDeepIronWeight;
    public static double dseDeepGoldWeight;
    public static int dseDeepMaxIronPieces;
    public static double dseSurfaceZombieWeaponChance;
    public static double dseMidZombieWeaponChance;
    public static double dseDeepZombieWeaponChance;

    // Balanced Elytra flags
    public static boolean enableBalancedElytra;
        public static int balancedElytraBlocksPerDurability;

    // Nerfed Discounts flags
    public static boolean enableNerfedDiscounts;
    public static boolean removeZombieCureDiscounts;
    public static double maxVillagerDiscount;

    // Mobs
    public static boolean enableCaveSpidersInCaves;
    public static double caveSpiderReplacementChance;
    
    // Improved Phantoms
    public static boolean enableImprovedPhantoms;
    public static boolean enablePhantomPickup;
    public static double phantomPickupChance;
    public static int phantomPickupMinDelay;
    public static double phantomPickupSpeedMultiplier;
    public static boolean enablePhantomSlownessStacking;
    public static int phantomSlownessDuration;
    public static int phantomSlownessMaxLevel;
    public static boolean enablePhantomDoubleDamage;
    public static boolean enablePhantomAutoDrop;
    public static int phantomAutoDropSeconds;
    public static boolean enablePhantomDropOnHit;
    public static boolean enablePhantomSlowFallingOnHitDismount;
    public static boolean enablePhantomSlowFallingLanding;
    public static int phantomSlowFallingLandingRange;

        // Iron Golems vs Creepers
        public static boolean enableIronGolemsKillCreepers;
    public static boolean enableBetterCombatIntegration;

    // Loot Tables
    public static boolean enableCustomLootTables;
    
    // Mineshaft loot
    public static boolean enableMineshaftLoot;
    public static boolean enableMineshaftResurfacingPotion;
    public static int mineshaftResurfacingWeight;
    public static int mineshaftResurfacingMinCount;
    public static int mineshaftResurfacingMaxCount;
    public static boolean enableMineshaftReturningPotion;
    public static int mineshaftReturningWeight;
    public static int mineshaftReturningMinCount;
    public static int mineshaftReturningMaxCount;
    public static boolean enableMineshaftHastePotion;
    public static int mineshaftHasteWeight;
    public static int mineshaftHasteMinCount;
    public static int mineshaftHasteMaxCount;
    public static boolean enableMineshaftStrongHastePotion;
    public static int mineshaftStrongHasteWeight;
    public static int mineshaftStrongHasteMinCount;
    public static int mineshaftStrongHasteMaxCount;
    
    // Stronghold loot
    public static boolean enableStrongholdLoot;
    public static boolean enableStrongholdResurfacingPotion;
    public static int strongholdResurfacingWeight;
    public static int strongholdResurfacingMinCount;
    public static int strongholdResurfacingMaxCount;
    public static boolean enableStrongholdReturningPotion;
    public static int strongholdReturningWeight;
    public static int strongholdReturningMinCount;
    public static int strongholdReturningMaxCount;
    
    // Ancient city loot
    public static boolean enableAncientCityLoot;
    public static boolean enableAncientCityResurfacingPotion;
    public static int ancientCityResurfacingWeight;
    public static int ancientCityResurfacingMinCount;
    public static int ancientCityResurfacingMaxCount;
    public static boolean enableAncientCityReturningPotion;
    public static int ancientCityReturningWeight;
    public static int ancientCityReturningMinCount;
    public static int ancientCityReturningMaxCount;
    
    // End city loot
    public static boolean enableEndCityLoot;
    public static boolean enableEndCityReturningPotion;
    public static int endCityReturningWeight;
    public static int endCityReturningMinCount;
    public static int endCityReturningMaxCount;
    
    // Simple dungeon loot
    public static boolean enableSimpleDungeonLoot;
    public static boolean enableSimpleDungeonResurfacingPotion;
    public static int simpleDungeonResurfacingWeight;
    public static int simpleDungeonResurfacingMinCount;
    public static int simpleDungeonResurfacingMaxCount;
    public static boolean enableSimpleDungeonReturningPotion;
    public static int simpleDungeonReturningWeight;
    public static int simpleDungeonReturningMinCount;
    public static int simpleDungeonReturningMaxCount;
    
    // Trial chamber loot
    public static boolean enableTrialChamberLoot;
    public static boolean enableTrialCommonResurfacingPotion;
    public static int trialCommonResurfacingWeight;
    public static int trialCommonResurfacingMinCount;
    public static int trialCommonResurfacingMaxCount;
    public static boolean enableTrialRareResurfacingPotion;
    public static int trialRareResurfacingWeight;
    public static int trialRareResurfacingMinCount;
    public static int trialRareResurfacingMaxCount;
    public static boolean enableTrialRareReturningPotion;
    public static int trialRareReturningWeight;
    public static int trialRareReturningMinCount;
    public static int trialRareReturningMaxCount;
    public static boolean enableTrialUniqueReturningPotion;
    public static int trialUniqueReturningWeight;
    public static int trialUniqueReturningMinCount;
    public static int trialUniqueReturningMaxCount;
    
    // Custom loot category
    public static boolean enableCustomLootCategory;
    public static int customCategoryWeight;
    
    // Mob loot tables
    public static boolean enableMobLootTables;
    public static boolean enableDrownedLootTable;
    public static boolean enableHuskLootTable;

    // Recipes flags
    public static boolean enableAlternativeRepeaterRecipe;
    public static boolean enableChestFromLogsRecipe;
    public static boolean enableMapInkSacRecipe;
    public static boolean enableRecoveryCompassRecipe;
    public static boolean enableEnderEyeRecipe;
    public static boolean enableRawIronSmeltingRecipe;
    public static boolean enableRawGoldSmeltingRecipe;
    public static boolean enableRawCopperSmeltingRecipe;
    public static boolean enableRawIronBlastingRecipe;
    public static boolean enableRawGoldBlastingRecipe;
    public static boolean enableRawCopperBlastingRecipe;
    public static boolean enableStairRecipeOverride;
    public static boolean enableCompactStairRecipe;

    // Fuel Tweaks
    public static boolean enableTorchFuel;
    public static int torchBurnTime;

        // Enchanting > Aerodynamic
        public static boolean enableAerodynamic;
        public static double aerodynamicBaseDragReduction;
        public static int aerodynamicMinAltitude;
        public static int aerodynamicMaxAltitude;
        public static double aerodynamicCloudLevel;
        public static double aerodynamicSpeedMultiplier;
        public static double aerodynamicMaxFlightSpeed;

    // Tweaks > Respawn Anchor Anywhere
    public static boolean enableRespawnAnchorAnywhere;

        // Tweaks > Return To Killer
        public static boolean enableReturnToKiller;

        // Tweaks > Tipped Arrows Lingering Clouds
        public static boolean enableTippedArrowLingeringClouds;

        // Tweaks > Speedy Happy Ghasts
        public static boolean enableSpeedyHappyGhasts;

        // Tweaks > Candle Bundles
        public static boolean allowMixedCandlePlacement;

    // Tweaks > Enhanced Slab Behavior
    public static boolean enableEnhancedSlabs;
    public static boolean enhancedSlabsPlaceOnTop;
    public static boolean enhancedSlabsHangBelow;
    public static boolean enhancedSlabsKneeSlabMining;
    public static boolean enhancedSlabsMixedDoubleSlabs;
    public static boolean enhancedSlabsVerticalSlabs;
    public static boolean enhancedSlabsSteps;


    // ========================================
    // VALIDATION METHODS
    // ========================================
    private static boolean validateEntityTypeName(final Object obj) {
                return obj instanceof String name && BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(name));
    }

    private static boolean validateItemName(final Object obj) {
                return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }

    private static boolean validateResourceLocationName(final Object obj) {
        if (!(obj instanceof String s)) return false;
        try {
                        Identifier.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Config values are loaded by the grouped config classes:
    // - GameplayConfig.onLoad() for gameplay, combat, balance, mechanics, tweaks, and mobs
    // - ContentConfig.onLoad() for glow goo, potions, building, enchanting, recipes, and loot tables
    // - ClientConfig.onLoad() for client features
}