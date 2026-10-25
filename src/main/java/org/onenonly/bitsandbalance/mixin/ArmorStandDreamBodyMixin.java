package org.onenonly.bitsandbalance.mixin;

//? if mc == 1.21.1
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandDreamBodyMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$redirectBodyDamage(DamageSource source, float amount,
                                                     CallbackInfoReturnable<Boolean> cir) {
        ArmorStand stand = (ArmorStand) (Object) this;
        if (!stand.level().isClientSide()
                && (DreamProjection.isBody(stand) || DreamProjection.isGhost(stand))) {
            cir.setReturnValue(DreamProjection.hurtBody(stand, source, amount));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$removeOrphanedBody(CallbackInfo ci) {
        ArmorStand stand = (ArmorStand) (Object) this;
        if (DreamProjection.isOrphanedBody(stand)) {
            stand.discard();
        }
    }
}
//? endif
