package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.entity.SpeedyHappyGhastSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.happyghast.HappyGhast$GhastMoveControl", remap = false)
public abstract class VanillaBackportHappyGhastMoveControlSpeedMixin extends MoveControl {
    @Unique
    private Vec3 bitsandbalance$velocityBeforeTick;

    protected VanillaBackportHappyGhastMoveControlSpeedMixin(Mob mob) {
        super(mob);
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureVelocityBeforeVanillaBackportMoveControlTick(CallbackInfo ci) {
        if (!Config.enableSpeedyHappyGhasts || !(this.mob instanceof LivingEntity livingEntity)) {
            bitsandbalance$velocityBeforeTick = null;
            return;
        }

        bitsandbalance$velocityBeforeTick = livingEntity.getDeltaMovement();
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void bitsandbalance$applySpeedEffectsToVanillaBackportMoveControlTick(CallbackInfo ci) {
        if (!Config.enableSpeedyHappyGhasts || !(this.mob instanceof LivingEntity livingEntity)) {
            bitsandbalance$velocityBeforeTick = null;
            return;
        }

        if (bitsandbalance$velocityBeforeTick == null) {
            return;
        }

        livingEntity.setDeltaMovement(
            SpeedyHappyGhastSupport.applyPotionAdjustedVelocityChange(
                livingEntity,
                bitsandbalance$velocityBeforeTick,
                livingEntity.getDeltaMovement()
            )
        );
        bitsandbalance$velocityBeforeTick = null;
    }
}