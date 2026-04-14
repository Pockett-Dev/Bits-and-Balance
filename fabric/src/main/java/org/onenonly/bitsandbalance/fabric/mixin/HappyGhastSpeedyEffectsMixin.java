package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HappyGhast.class)
public abstract class HappyGhastSpeedyEffectsMixin {

    @ModifyVariable(method = "travel", at = @At(value = "STORE"), ordinal = 0)
    private float bitsandbalance$applySpeedEffectsToFlyingSpeed(float f) {
        if (!FabricTweaksConfig.enableSpeedyHappyGhasts) return f;

        HappyGhast self = (HappyGhast) (Object) this;

        // Preferred path: drive scaling via the entity's Flying Speed attribute.
        // This allows Speed/Slowness to expose a visible "Flying Speed" modifier on potion items.
        try {
            AttributeInstance inst = self.getAttribute(Attributes.FLYING_SPEED);
            if (inst != null) {
                double base = inst.getBaseValue();
                double value = inst.getValue();
                if (base > 0.0D && value >= 0.0D) {
                    return f * (float) (value / base);
                }
            }
        } catch (Throwable ignored) {
        }

        float multiplier = 1.0F;

        try {
            var speed = self.getEffect(net.minecraft.world.effect.MobEffects.SPEED);
            if (speed != null) {
                int amp = speed.getAmplifier() + 1;
                multiplier *= 1.0F + 0.2F * amp;
            }

            var slowness = self.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
            if (slowness != null) {
                int amp = slowness.getAmplifier() + 1;
                multiplier *= Math.max(0.0F, 1.0F - 0.15F * amp);
            }
        } catch (Throwable ignored) {
        }

        return f * multiplier;
    }
}
