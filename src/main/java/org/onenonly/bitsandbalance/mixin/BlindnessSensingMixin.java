package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.potions.BlindnessMobVision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BlindnessSensingMixin {
    @Inject(method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$limitSight(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (BlindnessMobVision.isOutOfRange((LivingEntity) (Object) this, target)) {
            cir.setReturnValue(false);
        }
    }
}
