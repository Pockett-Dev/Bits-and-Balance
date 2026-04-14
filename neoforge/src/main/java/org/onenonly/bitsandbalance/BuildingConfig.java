package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class BuildingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static { BUILDER.comment("Building").push("building"); }

    static {
        BUILDER.comment(
            "Vertical Slabs",
            "Dynamic vertical slab variants crafted from slabs."
        ).push("verticalSlabs");
        }

        private static final ModConfigSpec.BooleanValue VERTICAL_SLABS_ENABLED = BUILDER
            .comment("Enable Vertical Slabs. Disabling this stops dynamic vertical slab generation and generated recipes, and turns off related placement and presentation.")
            .define("enabled", true);

        static { BUILDER.pop(); }

        static {
        BUILDER.comment(
                "Steps and Vertical Steps",
                "Quarter-block step variants crafted from slabs and vertical slabs."
        ).push("stepsAndVerticalSteps");
    }

    private static final ModConfigSpec.BooleanValue STEPS_AND_VERTICAL_STEPS_ENABLED = BUILDER
            .comment("Enable Steps and Vertical Steps. Disabling this stops dynamic step/vertical-step generation and generated recipes, and turns off related placement and presentation.")
            .define("enabled", true);

    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        Config.enhancedSlabsVerticalSlabs = VERTICAL_SLABS_ENABLED.get();
        Config.enhancedSlabsSteps = STEPS_AND_VERTICAL_STEPS_ENABLED.get();
    }
}