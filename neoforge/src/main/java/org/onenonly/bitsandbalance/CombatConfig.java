package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.stream.Collectors;

import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CombatConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Combat");
        BUILDER.push("combat");
    }

    // Second Chance
    static {
        BUILDER.comment("Second Chance");
        BUILDER.push("secondChance");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SECOND_CHANCE = BUILDER
            .comment("Enable the Second Chance anti-one-shot mechanic for players (excludes fall damage by default). ")
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

    // Apply Resistance
    static {
        BUILDER.comment("Apply Resistance effect when Second Chance triggers.");
        BUILDER.push("applyResistance");
    }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_RESISTANCE_ENABLED = BUILDER
            .comment("Enable applying Resistance when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_RESISTANCE_LEVEL = BUILDER
            .comment("Resistance effect level to apply (1 = Resistance I).")
            .defineInRange("level", 1, 1, 10);
    static { BUILDER.pop(); }

    // Apply Nausea
    static {
        BUILDER.comment("Apply Nausea effect when Second Chance triggers.");
        BUILDER.push("applyNausea");
    }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_NAUSEA_ENABLED = BUILDER
            .comment("Enable applying Nausea when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_NAUSEA_LEVEL = BUILDER
            .comment("Nausea effect level to apply (1 = Nausea I).")
            .defineInRange("level", 2, 1, 10);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    // Maintain Experience
    static {
        BUILDER.comment("Maintain Experience");
        BUILDER.push("maintainExperience");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MAINTAIN_EXPERIENCE = BUILDER
            .comment("Enable Maintain Experience: players lose a percentage of their experience on death.")
            .define("enableMaintainExperience", true);
    private static final ModConfigSpec.IntValue MAINTAIN_EXPERIENCE_LOSS_PERCENT = BUILDER
            .comment("Percentage of total experience to lose on death (0-100).\n Default: 50\n Range: 0 ~ 100")
            .defineInRange("lossPercent", 50, 0, 100);
    static { BUILDER.pop(); }

    // Snowball Rework
    static {
        BUILDER.comment("Snowball Rework");
        BUILDER.push("snowball");
    }
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
            .defineList("snowballNetherEntities",
                    List.of(
                            "minecraft:blaze",
                            "minecraft:ghast",
                            "minecraft:magma_cube",
                            "minecraft:strider",
                            "minecraft:wither_skeleton",
                            "minecraft:piglin",
                            "minecraft:piglin_brute",
                            "minecraft:zombified_piglin",
                            "minecraft:hoglin",
                            "minecraft:zoglin"
                    ), CombatConfig::validateEntityTypeName);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); } // exit combat

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateEntityTypeName(final Object obj) {
                return obj instanceof String name && BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(name));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        // Combat
        Config.enableSecondChance = ENABLE_SECOND_CHANCE.get();
        Config.secondChanceExcludeFall = SECOND_CHANCE_EXCLUDE_FALL.get();
        Config.secondChanceCooldownDays = SECOND_CHANCE_COOLDOWN_DAYS.get();
        Config.secondChanceResetOnRespawn = SECOND_CHANCE_RESET_ON_RESPAWN.get();
        Config.secondChanceResistanceEnabled = SECOND_CHANCE_RESISTANCE_ENABLED.get();
        Config.secondChanceResistanceLevel = SECOND_CHANCE_RESISTANCE_LEVEL.get();
        Config.secondChanceNauseaEnabled = SECOND_CHANCE_NAUSEA_ENABLED.get();
        Config.secondChanceNauseaLevel = SECOND_CHANCE_NAUSEA_LEVEL.get();

        Config.enableMaintainExperience = ENABLE_MAINTAIN_EXPERIENCE.get();
        Config.maintainExperienceLossPercent = MAINTAIN_EXPERIENCE_LOSS_PERCENT.get();

        Config.enableSnowballRework = ENABLE_SNOWBALL_REWORK.get();
        Config.snowballBaseDamage = SNOWBALL_BASE_DAMAGE.get();
        Config.snowballNetherDamage = SNOWBALL_NETHER_DAMAGE.get();
        Config.snowballMinFreezeSeconds = SNOWBALL_MIN_FREEZE_SECONDS.get();
        Config.snowballMaxFreezeSeconds = SNOWBALL_MAX_FREEZE_SECONDS.get();
        Config.snowballNetherEntityTypes = SNOWBALL_NETHER_ENTITIES.get().stream()
                .map(Identifier::parse)
                .map(BuiltInRegistries.ENTITY_TYPE::get)
                .flatMap(java.util.Optional::stream)
                .map(net.minecraft.core.Holder.Reference::value)
                .collect(Collectors.toSet());

    }
}