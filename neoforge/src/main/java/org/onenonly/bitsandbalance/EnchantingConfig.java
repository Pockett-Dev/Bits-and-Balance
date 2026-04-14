package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Enchanting-related configuration.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class EnchantingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========================================
    // AERODYNAMIC
    // ========================================
    private static final ModConfigSpec.BooleanValue ENABLE_AERODYNAMIC = BUILDER
            .comment("Enable the Aerodynamic enchantment effect.")
            .define("aerodynamic.enabled", true);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_BASE_DRAG_REDUCTION = BUILDER
            .comment("Base horizontal drag reduction at full strength. 0.5 = 50% less horizontal drag (recommended).")
            .defineInRange("aerodynamic.baseDragReduction", 0.5D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue AERODYNAMIC_MIN_ALTITUDE = BUILDER
            .comment("Minimum Y where Aerodynamic begins to take effect.")
            .defineInRange("aerodynamic.minAltitude", 192, -2032, 4096);

    private static final ModConfigSpec.IntValue AERODYNAMIC_MAX_ALTITUDE = BUILDER
            .comment("Y where Aerodynamic reaches full strength.")
            .defineInRange("aerodynamic.maxAltitude", 320, -2032, 4096);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLOUD_LEVEL = BUILDER
            .comment("Cloud level used as the midpoint of the curve.")
            .defineInRange("aerodynamic.cloudLevel", 192.0D, -2032.0D, 4096.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SPEED_MULTIPLIER = BUILDER
            .comment("Global multiplier on the effect strength.")
            .defineInRange("aerodynamic.speedMultiplier", 1.0D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_FLIGHT_SPEED = BUILDER
            .comment("Maximum elytra flight speed (m/s) while Aerodynamic is applying its boost.")
            .defineInRange("aerodynamic.maxFlightSpeed", 50.0D, 0.0D, 200.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;

        Config.enableAerodynamic = ENABLE_AERODYNAMIC.get();
        Config.aerodynamicBaseDragReduction = AERODYNAMIC_BASE_DRAG_REDUCTION.get();
        Config.aerodynamicMinAltitude = AERODYNAMIC_MIN_ALTITUDE.get();
        Config.aerodynamicMaxAltitude = AERODYNAMIC_MAX_ALTITUDE.get();
        Config.aerodynamicCloudLevel = AERODYNAMIC_CLOUD_LEVEL.get();
        Config.aerodynamicSpeedMultiplier = AERODYNAMIC_SPEED_MULTIPLIER.get();
                Config.aerodynamicMaxFlightSpeed = AERODYNAMIC_MAX_FLIGHT_SPEED.get();
    }
}
