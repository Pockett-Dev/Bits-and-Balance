package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import org.onenonly.bitsandbalance.common.entity.SpeedyHappyGhastSupport;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownPotion.class)
public abstract class HappyGhastSplashPotionMixin {

    @Inject(method = "applySplash(Ljava/lang/Iterable;Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void bitsandbalance$applySplashToHappyGhasts(Iterable<MobEffectInstance> effects, Entity directHitEntity, CallbackInfo ci) {
        if (!FabricTweaksConfig.enableSpeedyHappyGhasts) {
            return;
        }

        SpeedyHappyGhastSupport.applyMissingSplashPotionEffects((ThrownPotion) (Object) this, effects, directHitEntity);
    }
}