package org.onenonly.bitsandbalance.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a visible "Flying Speed" attribute modifier to Speed/Slowness.
 *
 * This is primarily so Swiftness/Slowness potions show that they affect flying speed,
 * enabling the Speedy Happy Ghasts feature to be discoverable from item tooltips.
 */
@Mixin(MobEffects.class)
public abstract class MobEffectsFlyingSpeedAttributeMixin {
    private static final Identifier BITSANDBALANCE_SPEED_FLYING_SPEED_ID =
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "speed_flying_speed");

    private static final Identifier BITSANDBALANCE_SLOWNESS_FLYING_SPEED_ID =
        Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "slowness_flying_speed");

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void bitsandbalance$addFlyingSpeedEffectModifiers(CallbackInfo ci) {
        try {
            // Match the existing Speedy Happy Ghasts multipliers:
            // Speed:   1 + 0.4 * (amplifier + 1)
            // Slowness: 1 - 0.15 * (amplifier + 1)
            MobEffects.SPEED.value().addAttributeModifier(
                Attributes.FLYING_SPEED,
                BITSANDBALANCE_SPEED_FLYING_SPEED_ID,
                0.2D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

            MobEffects.SLOWNESS.value().addAttributeModifier(
                Attributes.FLYING_SPEED,
                BITSANDBALANCE_SLOWNESS_FLYING_SPEED_ID,
                -0.15D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
        } catch (Throwable ignored) {
        }
    }
}
