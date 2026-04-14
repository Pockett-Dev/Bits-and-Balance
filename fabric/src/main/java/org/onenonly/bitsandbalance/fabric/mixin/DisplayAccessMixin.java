package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.math.Transformation;
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
}
