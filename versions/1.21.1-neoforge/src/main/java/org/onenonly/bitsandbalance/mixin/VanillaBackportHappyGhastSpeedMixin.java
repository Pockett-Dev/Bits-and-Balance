package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.entity.SpeedyHappyGhastSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.happyghast.HappyGhast", remap = false)
public abstract class VanillaBackportHappyGhastSpeedMixin {
    @Unique
    private Vec3 bitsandbalance$velocityBeforeTravel;

    @Inject(method = "travel", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureVelocityBeforeVanillaBackportTravel(Vec3 travelVector, CallbackInfo ci) {
        if (!Config.enableSpeedyHappyGhasts) {
            return;
        }

        bitsandbalance$velocityBeforeTravel = ((LivingEntity) (Object) this).getDeltaMovement();
    }

    @Inject(method = "travel", at = @At("TAIL"), require = 0)
    private void bitsandbalance$applySpeedEffectsToVanillaBackportTravel(Vec3 travelVector, CallbackInfo ci) {
        if (!Config.enableSpeedyHappyGhasts) {
            bitsandbalance$velocityBeforeTravel = null;
            return;
        }

        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (bitsandbalance$velocityBeforeTravel == null) {
            return;
        }

        livingEntity.setDeltaMovement(
            SpeedyHappyGhastSupport.applyPotionAdjustedVelocityChange(
                livingEntity,
                bitsandbalance$velocityBeforeTravel,
                livingEntity.getDeltaMovement()
            )
        );
        bitsandbalance$velocityBeforeTravel = null;
    }

    @Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$applySpeedEffectsToVanillaBackportRiddenInput(Player player, Vec3 travelVector, CallbackInfoReturnable<Vec3> cir) {
        if (!Config.enableSpeedyHappyGhasts) {
            return;
        }

        Vec3 current = cir.getReturnValue();
        if (current == null) {
            return;
        }

        float multiplier = SpeedyHappyGhastSupport.getPotionAdjustedFlyingSpeedMultiplier((LivingEntity) (Object) this);
        cir.setReturnValue(current.scale(multiplier));
    }
}