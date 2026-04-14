package org.onenonly.bitsandbalance.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.registry.ModAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enchanting: Aerodynamic
 *
 * Reduces horizontal air resistance during elytra flight based on the aerodynamic_drag attribute.
 * The attribute value (typically 0.5 for 50% drag reduction) is applied by the enchantment.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAerodynamicMixin {
    @Unique
    private Vec3 bitsandbalance$velocityBeforeTravel;

    @Inject(method = "travel", at = @At("HEAD"))
    private void bitsandbalance$captureVelocity(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!Config.enableAerodynamic) return;
        if (!self.isFallFlying()) return;
        
        // Store velocity before vanilla travel physics apply drag
        bitsandbalance$velocityBeforeTravel = self.getDeltaMovement();
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void bitsandbalance$applyAerodynamic(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!Config.enableAerodynamic) return;
        if (!self.isFallFlying()) return;
        if (bitsandbalance$velocityBeforeTravel == null) return;

        // Get the drag reduction from the attribute (enchantment applies this)
        double dragReduction = self.getAttributeValue(ModAttributes.AERODYNAMIC_DRAG);
        if (dragReduction <= 0.0D) return;

        // dragReduction is positive (e.g., 0.5 on the elytra)
        dragReduction = Math.max(0.0D, Math.min(1.0D, dragReduction));
        
        // Apply altitude scaling (config-driven)
        // - Below minAltitude: no effect
        // - Above maxAltitude: full effect
        // - cloudLevel is treated as the midpoint of the curve (altitudeFactor ~= 0.5)
        double y = self.getY();
        double minY = (double) Config.aerodynamicMinAltitude;
        double maxY = (double) Config.aerodynamicMaxAltitude;
        double cloudY = Config.aerodynamicCloudLevel;

        double altitudeFactor;
        if (y <= minY) {
            altitudeFactor = 0.0D;
        } else if (y >= maxY) {
            altitudeFactor = 1.0D;
        } else {
            double denom = Math.max(1.0D, maxY - minY);
            double t = (y - minY) / denom; // 0..1

            // Map t so that t==mid maps to 0.5 (piecewise linear), where mid is derived from cloudY.
            double mid = (cloudY - minY) / denom;
            mid = Math.max(0.0D, Math.min(1.0D, mid));

            if (mid <= 0.0D || mid >= 1.0D) {
                altitudeFactor = t;
            } else if (t <= mid) {
                altitudeFactor = 0.5D * (t / mid);
            } else {
                altitudeFactor = 0.5D + 0.5D * ((t - mid) / (1.0D - mid));
            }
        }

        // Scale by config base strength (calibrated so 0.5 stays 0.5) + altitude + multiplier
        double baseScale = Config.aerodynamicBaseDragReduction / 0.5D;
        double effectiveDragReduction = dragReduction * baseScale * altitudeFactor * Config.aerodynamicSpeedMultiplier;
        effectiveDragReduction = Math.max(0.0D, Math.min(1.0D, effectiveDragReduction));

        Vec3 velocityAfter = self.getDeltaMovement();
        Vec3 velocityBefore = bitsandbalance$velocityBeforeTravel;

        // Strongest when the player is gliding horizontally:
        // - pitch near 0 degrees
        // - movement is mostly horizontal (vy small)
        double pitchAbs = Math.abs(self.getXRot());
        double pitchFactor = 1.0D - Math.min(1.0D, pitchAbs / 60.0D);
        pitchFactor = pitchFactor * pitchFactor;

        Vec3 horizAfter = new Vec3(velocityAfter.x, 0.0D, velocityAfter.z);
        double horizSpeedAfter = horizAfter.length();
        double totalSpeedAfter = velocityAfter.length();
        double motionHorizFactor = totalSpeedAfter <= 1.0E-6D ? 0.0D : (horizSpeedAfter / totalSpeedAfter);
        motionHorizFactor = motionHorizFactor * motionHorizFactor;

        double glideFactor = pitchFactor * motionHorizFactor;
        double glideScaledReduction = effectiveDragReduction * glideFactor;

        // Restore any lost horizontal speed (safe: never subtracts if speed increased this tick)
        Vec3 horizBefore = new Vec3(velocityBefore.x, 0.0D, velocityBefore.z);
        double horizSpeedBefore = horizBefore.length();
        double lostHorizSpeed = Math.max(0.0D, horizSpeedBefore - horizSpeedAfter);

        Vec3 horizDir = horizSpeedAfter <= 1.0E-6D ? new Vec3(0.0D, 0.0D, 0.0D) : horizAfter.scale(1.0D / horizSpeedAfter);
        double restore = lostHorizSpeed * glideScaledReduction;

        // Additional forward streamlining boost (more noticeable at altitude and when level)
        // Tuned to be clearly measurable without becoming runaway acceleration.
        double baseBoostPerTick = 0.10D; // blocks/tick at 100% (≈ 2.0 m/s)
        double boost = baseBoostPerTick * glideScaledReduction;
        boost = Math.min(boost, 0.22D);

        Vec3 boostedHoriz = horizAfter.add(horizDir.scale(restore + boost));
        double boostedX = boostedHoriz.x;
        double boostedZ = boostedHoriz.z;

        Vec3 finalVelocity = new Vec3(boostedX, velocityAfter.y, boostedZ);
        double maxMps = Config.aerodynamicMaxFlightSpeed;
        if (maxMps > 0.0D) {
            double maxBlocksPerTick = maxMps / 20.0D;
            double len = finalVelocity.length();
            if (len > maxBlocksPerTick && len > 1.0E-6D) {
                finalVelocity = finalVelocity.scale(maxBlocksPerTick / len);
                boostedX = finalVelocity.x;
                boostedZ = finalVelocity.z;
            }
        }

        self.setDeltaMovement(finalVelocity);

        bitsandbalance$velocityBeforeTravel = null;
    }
}
