package org.onenonly.bitsandbalance.mixin;

import com.mojang.math.Transformation;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.util.Brightness;
//? endif
import net.minecraft.world.entity.Display;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceDisplayAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessMixin extends BitsAndBalanceDisplayAccess {
    @Override
    @Invoker("setTransformation")
    void bitsandbalance$setTransformation(Transformation transformation);

    @Override
    @Invoker("setTransformationInterpolationDuration")
    void bitsandbalance$setTransformationInterpolationDuration(int ticks);

    @Override
    @Invoker("setTransformationInterpolationDelay")
    void bitsandbalance$setTransformationInterpolationDelay(int ticks);

    @Override
    @Invoker("setGlowColorOverride")
    void bitsandbalance$setGlowColorOverride(int argb);

    @Override
    @Invoker("getGlowColorOverride")
    int bitsandbalance$getGlowColorOverride();

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
    @Override
    @Invoker("setBrightnessOverride")
    void bitsandbalance$setBrightnessOverride(Brightness brightness);
//? endif
}
