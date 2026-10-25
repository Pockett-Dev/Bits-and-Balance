package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.onenonly.bitsandbalance.common.potions.BlindnessMobVision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class BlindnessTargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$limitTargetRange(LivingEntity observer,
                                               LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (BlindnessMobVision.isOutOfRange(observer, target)) {
            cir.setReturnValue(false);
        }
    }
}
