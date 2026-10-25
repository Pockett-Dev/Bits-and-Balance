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
            .comment("Y where Aerodynamic reaches full strength; below this it still applies at reduced strength.")
            .defineInRange("aerodynamic.minAltitude", 192, -2032, 4096);

    private static final ModConfigSpec.IntValue AERODYNAMIC_MAX_ALTITUDE = BUILDER
            .comment("Legacy ramp reference. The distance from minAltitude controls how gradually Aerodynamic fades below full strength.")
            .defineInRange("aerodynamic.maxAltitude", 320, -2032, 4096);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLOUD_LEVEL = BUILDER
            .comment("Legacy setting kept for config compatibility. Full strength is controlled by minAltitude.")
            .defineInRange("aerodynamic.cloudLevel", 192.0D, -2032.0D, 4096.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SPEED_MULTIPLIER = BUILDER
            .comment("Global multiplier on the effect strength.")
            .defineInRange("aerodynamic.speedMultiplier", 1.0D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_FLIGHT_SPEED = BUILDER
            .comment("Maximum Aerodynamic-modified elytra flight speed (m/s) at full altitude strength. Scales down below full-strength altitude.")
            .defineInRange("aerodynamic.maxFlightSpeed", 50.0D, 0.0D, 200.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_LOW_ALTITUDE_STRENGTH_FLOOR = BUILDER
            .comment("Minimum Aerodynamic strength below the altitude ramp. 0.5 = at least half strength.")
            .defineInRange("aerodynamic.lowAltitudeStrengthFloor", 0.5D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_GLIDE_PITCH_RANGE = BUILDER
            .comment("Pitch angle in degrees where level-glide efficiency fades out. Larger values make the glide bonus apply over steeper pitch.")
            .defineInRange("aerodynamic.glidePitchRange", 60.0D, 1.0D, 180.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_PITCH_START = BUILDER
            .comment("Upward pitch angle in degrees where climb efficiency starts.")
            .defineInRange("aerodynamic.upwardPitchStart", 5.0D, 0.0D, 90.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_PITCH_RANGE = BUILDER
            .comment("Additional upward pitch range in degrees needed to reach full climb pitch efficiency.")
            .defineInRange("aerodynamic.upwardPitchRange", 40.0D, 1.0D, 180.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_MOTION_FULL_SPEED = BUILDER
            .comment("Upward velocity in blocks per tick that counts as full climb motion efficiency.")
            .defineInRange("aerodynamic.upwardMotionFullSpeed", 0.25D, 0.01D, 10.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_ASSIST_SCALE = BUILDER
            .comment("How much climb efficiency keeps drag reduction active while pitched upward.")
            .defineInRange("aerodynamic.climbAssistScale", 0.2D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_SCALE = BUILDER
            .comment("How strongly Aerodynamic reduces downward sink from its current drag-reduction strength.")
            .defineInRange("aerodynamic.sinkReductionScale", 0.6D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_SINK_REDUCTION = BUILDER
            .comment("Maximum fraction of downward sink Aerodynamic can remove. 0.75 = up to 75% less sink.")
            .defineInRange("aerodynamic.maxSinkReduction", 0.75D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_MIN_HORIZONTAL_SPEED = BUILDER
            .comment("Horizontal speed in blocks per tick where sink reduction starts ramping in.")
            .defineInRange("aerodynamic.sinkReductionMinHorizontalSpeed", 0.5D, 0.0D, 20.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_FULL_HORIZONTAL_SPEED = BUILDER
            .comment("Horizontal speed in blocks per tick where sink reduction reaches full configured strength.")
            .defineInRange("aerodynamic.sinkReductionFullHorizontalSpeed", 1.5D, 0.01D, 20.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_RESTORE_CAP = BUILDER
            .comment("Maximum fraction of upward speed loss that Aerodynamic can restore per tick.")
            .defineInRange("aerodynamic.climbRestoreCap", 0.15D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_RESTORE_SCALE = BUILDER
            .comment("How strongly climb efficiency restores upward speed loss.")
            .defineInRange("aerodynamic.climbRestoreScale", 0.3D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_TURN_RESPONSE = BUILDER
            .comment("Direction-change amount needed to reach full turn speed preservation bonus. Lower values make turns count sooner.")
            .defineInRange("aerodynamic.turnResponse", 0.5D, 0.01D, 2.0D);

    private static final ModConfigSpec.DoubleValue AERODYNAMIC_TURN_RESTORE_BONUS = BUILDER
            .comment("Extra multiplier applied to lost horizontal speed restoration while turning.")
            .defineInRange("aerodynamic.turnRestoreBonus", 0.25D, 0.0D, 10.0D);

    private static final ModConfigSpec.BooleanValue ENABLE_ENCHANCEMENT_ELYTRA_DURABILITY_COMPAT = BUILDER
            .comment("When Enchancement is installed, keep vanilla Elytra durability loss even if Enchancement's Disable Durability setting is enabled.")
            .define("aerodynamic.enchancementElytraDurabilityCompat", true);

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
        Config.aerodynamicLowAltitudeStrengthFloor = AERODYNAMIC_LOW_ALTITUDE_STRENGTH_FLOOR.get();
        Config.aerodynamicGlidePitchRange = AERODYNAMIC_GLIDE_PITCH_RANGE.get();
        Config.aerodynamicUpwardPitchStart = AERODYNAMIC_UPWARD_PITCH_START.get();
        Config.aerodynamicUpwardPitchRange = AERODYNAMIC_UPWARD_PITCH_RANGE.get();
        Config.aerodynamicUpwardMotionFullSpeed = AERODYNAMIC_UPWARD_MOTION_FULL_SPEED.get();
        Config.aerodynamicClimbAssistScale = AERODYNAMIC_CLIMB_ASSIST_SCALE.get();
        Config.aerodynamicSinkReductionScale = AERODYNAMIC_SINK_REDUCTION_SCALE.get();
        Config.aerodynamicMaxSinkReduction = AERODYNAMIC_MAX_SINK_REDUCTION.get();
        Config.aerodynamicSinkReductionMinHorizontalSpeed = AERODYNAMIC_SINK_REDUCTION_MIN_HORIZONTAL_SPEED.get();
        Config.aerodynamicSinkReductionFullHorizontalSpeed = AERODYNAMIC_SINK_REDUCTION_FULL_HORIZONTAL_SPEED.get();
        Config.aerodynamicClimbRestoreCap = AERODYNAMIC_CLIMB_RESTORE_CAP.get();
        Config.aerodynamicClimbRestoreScale = AERODYNAMIC_CLIMB_RESTORE_SCALE.get();
        Config.aerodynamicTurnResponse = AERODYNAMIC_TURN_RESPONSE.get();
        Config.aerodynamicTurnRestoreBonus = AERODYNAMIC_TURN_RESTORE_BONUS.get();
        Config.enableEnchancementElytraDurabilityCompat = ENABLE_ENCHANCEMENT_ELYTRA_DURABILITY_COMPAT.get();
    }
}
