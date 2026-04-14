package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class PotionsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Potions");
        BUILDER.push("potions");
    }

    // Withering Potion
    static {
        BUILDER.comment("Withering Potion");
        BUILDER.push("withering");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_WITHERING_POTION = BUILDER
            .comment("Enable brewing for Withering potions.",
                    "Recipe: Poison/Harming Potion + Wither Rose = Withering Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableWitheringPotion", true);
    static { BUILDER.pop(); }

    // Resurfacing Potion
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacing");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_RESURFACING_POTION = BUILDER
            .comment("Enable the Resurfacing potion effect (teleports player to surface).",
                    "Recipe: Currently no brewing recipe - obtained via creative mode or commands.")
            .define("enableResurfacingPotion", true);
    static { BUILDER.pop(); }

    // Displacement Potion
    static {
        BUILDER.comment("Displacement Potion");
        BUILDER.push("displacement");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_DISPLACEMENT_POTION = BUILDER
            .comment("Enable the Displacement potion effect (randomly teleports player within 100-block radius).",
                    "Recipe: Awkward Potion + Ender Eye = Displacement Potion")
            .define("enableDisplacementPotion", true);
    static { BUILDER.pop(); }

    // Returning Potion
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returning");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_RETURNING_POTION = BUILDER
            .comment("Enable the Returning potion effect (teleports player to their latest death location).",
                    "Recipe: Displacement Potion + Echo Shard = Returning Potion")
            .define("enableReturningPotion", true);
    static { BUILDER.pop(); }

    // Levitation Potion
    static {
        BUILDER.comment("Levitation Potion");
        BUILDER.push("levitation");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_LEVITATION_POTION = BUILDER
            .comment("Enable brewing for Levitation potions.",
                    "Recipe: Slow Falling Potion + Shulker Shell = Levitation Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableLevitationPotion", true);
    static { BUILDER.pop(); }

    // Haste Potion
    static {
        BUILDER.comment("Haste Potion");
        BUILDER.push("haste");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_HASTE_POTION = BUILDER
            .comment("Enable brewing for Haste potions.",
                    "Recipe: Speed Potion + Quartz = Haste Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableHastePotion", true);
    static { BUILDER.pop(); }

    // Glowing Potion
    static {
        BUILDER.comment("Glowing Potion");
        BUILDER.push("glowing");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOWING_POTION = BUILDER
            .comment("Enable brewing for Glowing potions.",
                    "Recipe: Awkward Potion + Glow Berries = Glowing Potion",
                    "Upgrades: Redstone for extended variants")
            .define("enableGlowingPotion", true);
    static { BUILDER.pop(); }

    // Bioluminescence Potion
    static {
        BUILDER.comment("Bioluminescence Potion");
        BUILDER.push("bioluminescence");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_BIOLUMINESCENCE_POTION = BUILDER
            .comment("Enable brewing for Bioluminescence potions.",
                    "Recipe: Glowing Potion + Glow Goo = Bioluminescence Potion",
                    "Upgrades: Glowstone for stronger variants, Redstone for extended variants")
            .define("enableBioluminescencePotion", true);
    static { BUILDER.pop(); BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        Config.enableWitheringPotion = ENABLE_WITHERING_POTION.get();
        Config.enableResurfacingPotion = ENABLE_RESURFACING_POTION.get();
        Config.enableDisplacementPotion = ENABLE_DISPLACEMENT_POTION.get();
        Config.enableReturningPotion = ENABLE_RETURNING_POTION.get();
        Config.enableLevitationPotion = ENABLE_LEVITATION_POTION.get();
        Config.enableHastePotion = ENABLE_HASTE_POTION.get();
                Config.enableGlowingPotion = ENABLE_GLOWING_POTION.get();
                Config.enableBioluminescencePotion = ENABLE_BIOLUMINESCENCE_POTION.get();
    }
}
