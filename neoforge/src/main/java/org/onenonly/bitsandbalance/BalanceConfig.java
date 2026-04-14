package org.onenonly.bitsandbalance;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.stream.Collectors;

import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class BalanceConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Balance");
        BUILDER.push("balance");
    }

    // Ender Dragon XP
    static {
        BUILDER.comment("Ender Dragon XP");
        BUILDER.push("enderDragon");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_FULL_ENDER_DRAGON_XP = BUILDER
            .comment("Always drop the full first-kill XP from the Ender Dragon.")
            .define("enableFullEnderDragonXp", true);
    static { BUILDER.pop(); }

    // Food
    static {
        BUILDER.comment("Food");
        BUILDER.push("food");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_FOOD_ALWAYS_EDIBLE = BUILDER
            .comment("Allow players to eat unconditionally.")
            .define("enableFoodAlwaysEdible", true);
    static { BUILDER.pop(); }

    // Source Dependent Invulnerability Frames
    static {
        BUILDER.comment("Source Dependent Invulnerability Frames");
        BUILDER.push("sourceDependentIFrames");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SOURCE_DEPENDENT_IFRAMES = BUILDER
            .comment("Allow entities to take damage multiple times within i-frames if damage comes from different sources.")
            .define("enabled", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SOURCE_IFRAME_BLACKLIST = BUILDER
            .comment("Damage types to never bypass i-frames (resource locations).")
            .define("blacklist", List.of(), obj -> {
                if (!(obj instanceof List<?> list)) return false;
                return list.stream().allMatch(item -> {
                    if (!(item instanceof String str)) return false;
                    return BalanceConfig.validateResourceLocationName(str);
                });
            });
    static { BUILDER.pop(); }

    // Depth Scaling Enemies
    static {
        BUILDER.comment("Depth Scaling Enemies");
        BUILDER.push("depthScalingEnemies");
    }
    private static final ModConfigSpec.BooleanValue DSE_ENABLED = BUILDER
            .comment("Enable depth-based scaling for Zombies and Skeletons in the Overworld.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue DSE_OVERWORLD_ONLY = BUILDER
            .comment("Restrict depth scaling to the Overworld only.")
            .define("overworldOnly", true);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_SURFACE_TIER = BUILDER
            .comment("If false, disables the surface tier from affecting spawns (no gear/weapon changes for mobs in the surface band).")
            .define("enableSurfaceTier", false);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_MID_TIER = BUILDER
            .comment("If false, disables the mid tier from affecting spawns (no gear/weapon changes for mobs in the mid-depth band).")
            .define("enableMidTier", true);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_DEEP_TIER = BUILDER
            .comment("If false, disables the deep tier from affecting spawns (no gear/weapon changes for mobs in the deep band).")
            .define("enableDeepTier", true);
    private static final ModConfigSpec.IntValue DSE_MID_Y = BUILDER
            .comment("Y-level threshold for mid depth tier (<= this is considered mid).")
            .defineInRange("midDepthY", 40, -2048, 4096);
    private static final ModConfigSpec.IntValue DSE_DEEP_Y = BUILDER
            .comment("Y-level threshold for deep tier (<= this is considered deep). Example: -16.")
            .defineInRange("deepDepthY", 0, -2048, 4096);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_EQUIP_CHANCE = BUILDER
            .comment("Chance per armor slot to equip a piece at surface tier (0.0-1.0).")
            .defineInRange("surfaceEquipChance", 0.10D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_EQUIP_CHANCE = BUILDER
            .comment("Chance per armor slot to equip a piece at mid tier (0.0-1.0).")
            .defineInRange("midEquipChance", 0.25D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_EQUIP_CHANCE = BUILDER
            .comment("Chance per armor slot to equip a piece at deep tier (0.0-1.0).")
            .defineInRange("deepEquipChance", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_LEATHER = BUILDER
            .comment("Surface tier: probability weight for Leather armor when equipping.")
            .defineInRange("surfaceLeatherWeight", 0.80D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_CHAIN = BUILDER
            .comment("Surface tier: probability weight for Chain armor when equipping.")
            .defineInRange("surfaceChainWeight", 0.15D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_GOLD = BUILDER
            .comment("Surface tier: probability weight for Gold armor when equipping.")
            .defineInRange("surfaceGoldWeight", 0.05D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_IRON = BUILDER
            .comment("Surface tier: probability weight for Iron armor when equipping.")
            .defineInRange("surfaceIronWeight", 0.00D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_LEATHER = BUILDER
            .comment("Mid tier: probability weight for Leather armor when equipping.")
            .defineInRange("midLeatherWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_CHAIN = BUILDER
            .comment("Mid tier: probability weight for Chain armor when equipping.")
            .defineInRange("midChainWeight", 0.25D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_GOLD = BUILDER
            .comment("Mid tier: probability weight for Gold armor when equipping.")
            .defineInRange("midGoldWeight", 0.05D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_IRON = BUILDER
            .comment("Mid tier: probability weight for Iron armor when equipping.")
            .defineInRange("midIronWeight", 0.2D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_LEATHER = BUILDER
            .comment("Deep tier: probability weight for Leather armor when equipping.")
            .defineInRange("deepLeatherWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_CHAIN = BUILDER
            .comment("Deep tier: probability weight for Chain armor when equipping.")
            .defineInRange("deepChainWeight", 0.45D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_GOLD = BUILDER
            .comment("Deep tier: probability weight for Gold armor when equipping.")
            .defineInRange("deepGoldWeight", 0.15D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_IRON = BUILDER
            .comment("Deep tier: probability weight for Iron armor when equipping. Note: hard-capped by deepMaxIronPieces.")
            .defineInRange("deepIronWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue DSE_DEEP_MAX_IRON = BUILDER
            .comment("Deep tier: maximum number of Iron armor pieces allowed on a single mob (example requires 1).")
            .defineInRange("deepMaxIronPieces", 1, 0, 4);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_ZOMBIE_WEAPON = BUILDER
            .comment("Surface tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).")
            .defineInRange("surfaceZombieWeaponChance", 0.05D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_ZOMBIE_WEAPON = BUILDER
            .comment("Mid tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).")
            .defineInRange("midZombieWeaponChance", 0.15D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_ZOMBIE_WEAPON = BUILDER
            .comment("Deep tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).")
            .defineInRange("deepZombieWeaponChance", 0.3D, 0.0D, 1.0D);
    static { BUILDER.pop(); }

    // Balanced Elytra
    static {
        BUILDER.comment("Balanced Elytra");
        BUILDER.push("balancedElytra");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_BALANCED_ELYTRA = BUILDER
            .comment("Enable Balanced Elytra: elytras lose durability based on distance flown (10 blocks = 1 durability) instead of time.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue BALANCED_ELYTRA_BLOCKS_PER_DURABILITY = BUILDER
            .comment("How many horizontal blocks flown equals 1 durability point (default: 10). Lower = faster wear.")
            .defineInRange("blocksPerDurability", 10, 1, 1000);
    static { BUILDER.pop(); }

        // Nerfed Mending
        static {
                BUILDER.comment("Nerfed Mending");
                BUILDER.push("nerfedMending");
        }
        private static final ModConfigSpec.BooleanValue ENABLE_NERFED_MENDING = BUILDER
                        .comment("Enable nerfed Mending: items require more XP to repair.")
                        .define("enabled", true);
        private static final ModConfigSpec.IntValue NERFED_MENDING_MULTIPLIER = BUILDER
                        .comment("XP multiplier for Mending repairs (2-10). Example: 4 = requires 4x XP for the same repair.")
                        .defineInRange("multiplier", 4, 2, 10);
        static { BUILDER.pop(); }

    // Nerfed Discounts
    static {
        BUILDER.comment("Nerfed Discounts");
        BUILDER.push("nerfedDiscounts");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_NERFED_DISCOUNTS = BUILDER
            .comment("Enable nerfed villager discounts system.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue REMOVE_ZOMBIE_CURE_DISCOUNTS = BUILDER
            .comment("Remove discounts gained from curing zombie villagers.")
            .define("removeZombieCureDiscounts", false);
    private static final ModConfigSpec.DoubleValue MAX_VILLAGER_DISCOUNT = BUILDER
            .comment("Maximum discount percentage any villager can offer (0.0-1.0, where 0.5 = 50%).")
            .defineInRange("maxDiscount", 0.5D, 0.0D, 1.0D);
    static { BUILDER.pop(); BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateResourceLocationName(final Object obj) {
        if (!(obj instanceof String s)) return false;
        try {
                        Identifier.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        // Balance
        Config.enableFullEnderDragonXp = ENABLE_FULL_ENDER_DRAGON_XP.get();
        Config.enableFoodAlwaysEdible = ENABLE_FOOD_ALWAYS_EDIBLE.get();
        Config.enableSourceDependentIFrames = ENABLE_SOURCE_DEPENDENT_IFRAMES.get();
        Config.sourceIFrameBlacklist = SOURCE_IFRAME_BLACKLIST.get().stream()
                .map(Identifier::parse)
                .collect(Collectors.toSet());

        // Depth Scaling Enemies
        Config.dseEnabled = DSE_ENABLED.get();
        Config.dseOverworldOnly = DSE_OVERWORLD_ONLY.get();
        Config.dseEnableSurfaceTier = DSE_ENABLE_SURFACE_TIER.get();
        Config.dseEnableMidTier = DSE_ENABLE_MID_TIER.get();
        Config.dseEnableDeepTier = DSE_ENABLE_DEEP_TIER.get();
        Config.dseMidDepthY = DSE_MID_Y.get();
        Config.dseDeepDepthY = DSE_DEEP_Y.get();
        Config.dseSurfaceEquipChance = DSE_SURFACE_EQUIP_CHANCE.get();
        Config.dseMidEquipChance = DSE_MID_EQUIP_CHANCE.get();
        Config.dseDeepEquipChance = DSE_DEEP_EQUIP_CHANCE.get();
        Config.dseSurfaceLeatherWeight = DSE_SURFACE_LEATHER.get();
        Config.dseSurfaceChainWeight = DSE_SURFACE_CHAIN.get();
        Config.dseSurfaceIronWeight = DSE_SURFACE_IRON.get();
        Config.dseSurfaceGoldWeight = DSE_SURFACE_GOLD.get();
        Config.dseMidLeatherWeight = DSE_MID_LEATHER.get();
        Config.dseMidChainWeight = DSE_MID_CHAIN.get();
        Config.dseMidIronWeight = DSE_MID_IRON.get();
        Config.dseMidGoldWeight = DSE_MID_GOLD.get();
        Config.dseDeepLeatherWeight = DSE_DEEP_LEATHER.get();
        Config.dseDeepChainWeight = DSE_DEEP_CHAIN.get();
        Config.dseDeepIronWeight = DSE_DEEP_IRON.get();
        Config.dseDeepGoldWeight = DSE_DEEP_GOLD.get();
        Config.dseDeepMaxIronPieces = DSE_DEEP_MAX_IRON.get();
        Config.dseSurfaceZombieWeaponChance = DSE_SURFACE_ZOMBIE_WEAPON.get();
        Config.dseMidZombieWeaponChance = DSE_MID_ZOMBIE_WEAPON.get();
        Config.dseDeepZombieWeaponChance = DSE_DEEP_ZOMBIE_WEAPON.get();

        // Balanced Elytra
        Config.enableBalancedElytra = ENABLE_BALANCED_ELYTRA.get();
        Config.balancedElytraBlocksPerDurability = BALANCED_ELYTRA_BLOCKS_PER_DURABILITY.get();

        // Nerfed Mending
        Config.enableNerfedMending = ENABLE_NERFED_MENDING.get();
        Config.nerfedMendingMultiplier = NERFED_MENDING_MULTIPLIER.get();

        // Nerfed Discounts
        Config.enableNerfedDiscounts = ENABLE_NERFED_DISCOUNTS.get();
        Config.removeZombieCureDiscounts = REMOVE_ZOMBIE_CURE_DISCOUNTS.get();
        Config.maxVillagerDiscount = MAX_VILLAGER_DISCOUNT.get();
    }
}
