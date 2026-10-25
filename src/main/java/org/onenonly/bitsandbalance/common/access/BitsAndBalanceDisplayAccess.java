package org.onenonly.bitsandbalance.common.access;

import com.mojang.math.Transformation;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.util.Brightness;
//? endif

public interface BitsAndBalanceDisplayAccess {
    void bitsandbalance$setTransformation(Transformation transformation);

    void bitsandbalance$setTransformationInterpolationDuration(int ticks);

    void bitsandbalance$setTransformationInterpolationDelay(int ticks);

    void bitsandbalance$setGlowColorOverride(int argb);

    int bitsandbalance$getGlowColorOverride();

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    void bitsandbalance$setBrightnessOverride(Brightness brightness);
//? endif
}
