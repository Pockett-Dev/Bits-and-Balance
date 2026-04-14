package org.onenonly.bitsandbalance.common.access;

import com.mojang.math.Transformation;

public interface BitsAndBalanceDisplayAccess {
    void bitsandbalance$setTransformation(Transformation transformation);

    void bitsandbalance$setTransformationInterpolationDuration(int ticks);

    void bitsandbalance$setTransformationInterpolationDelay(int ticks);

    void bitsandbalance$setGlowColorOverride(int argb);

    int bitsandbalance$getGlowColorOverride();
}
