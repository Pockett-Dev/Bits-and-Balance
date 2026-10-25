package org.onenonly.bitsandbalance.common.mechanics;

/**
 * Loader-agnostic runtime settings for Bottle of Cloud.
 *
 * NeoForge updates these from `MechanicsConfig`.
 * Fabric updates these from `FabricMechanicsConfig`.
 */
public final class BottleOfCloudRuntime {
    public static volatile boolean enabled = true;
    public static volatile int durationTicks = 400;
    public static volatile int fadeStartTicks = 100;
    public static volatile double placementDistance = 2.0D;
    public static volatile double fallDamageMultiplier = 0.5D;

    private BottleOfCloudRuntime() {
    }
}
