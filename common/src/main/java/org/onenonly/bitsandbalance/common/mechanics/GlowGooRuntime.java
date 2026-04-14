package org.onenonly.bitsandbalance.common.mechanics;

public final class GlowGooRuntime {
    public static volatile boolean enabled = true;
    public static volatile boolean impactParticlesEnabled = true;
    public static volatile int impactParticleCount = 16;
    public static volatile boolean splatterAmbientParticlesEnabled = true;
    public static volatile boolean bioluminescenceEnabled = true;
    public static volatile int bioluminescenceDurationTicks = 60 * 20;
    public static volatile int bioluminescenceLightLevel = 14;

    private GlowGooRuntime() {
    }
}