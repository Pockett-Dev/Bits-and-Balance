package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.util.Mth;

public final class BioluminescenceFadeHelper {
    private static final int FADE_DURATION_TICKS = 5 * 20;

    private BioluminescenceFadeHelper() {
    }

    public static double getFadeStrength(int remainingDurationTicks) {
        if (remainingDurationTicks <= 0) {
            return 0.0D;
        }
        if (remainingDurationTicks >= FADE_DURATION_TICKS) {
            return 1.0D;
        }

        double progress = Mth.clamp(remainingDurationTicks / (double) FADE_DURATION_TICKS, 0.0D, 1.0D);
        return progress * progress * (3.0D - (2.0D * progress));
    }

    public static double getDesiredValue(int remainingDurationTicks, double baseValue) {
        if (baseValue <= 0.0D) {
            return 0.0D;
        }

        return baseValue * getFadeStrength(remainingDurationTicks);
    }

    public static double getDesiredLightLevel(int remainingDurationTicks, int baseLightLevel) {
        double clampedBaseLightLevel = Mth.clamp(baseLightLevel, 0, 15);
        return getDesiredValue(remainingDurationTicks, clampedBaseLightLevel);
    }
}