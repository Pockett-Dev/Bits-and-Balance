package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.enchantments.ModEnchantments;
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

        // Prefer the data-driven attribute path (enchantment JSON applies aerodynamic_drag).
        double dragReduction = self.getAttributeValue(ModAttributes.AERODYNAMIC_DRAG);
        if (dragReduction <= 0.0D) {
            ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty() && chest.getItem() == Items.ELYTRA) {
                try {
                    var enchantmentLookup = self.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    var aerodynamicHolder = enchantmentLookup.get(ModEnchantments.AERODYNAMIC).orElse(null);
                    if (aerodynamicHolder != null) {
                        ItemEnchantments enchantments = chest.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                        int level = enchantments.getLevel(aerodynamicHolder);
                        if (level > 0) {
                            dragReduction = 0.5D;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (dragReduction <= 0.0D) {
            bitsandbalance$velocityBeforeTravel = null;
            return;
        }

        // dragReduction is positive (e.g., 0.5 on the elytra)
        dragReduction = Math.max(0.0D, Math.min(1.0D, dragReduction));
        
        // Apply altitude scaling (config-driven).
        // - At/above minAltitude: full effect
        // - Below minAltitude: reduced effect that fades down over the configured altitude span
        double y = self.getY();
        double fullStrengthY = (double) Config.aerodynamicMinAltitude;
        double rampSpan = Math.max(1.0D, Math.abs((double) Config.aerodynamicMaxAltitude - fullStrengthY));

        double minimumAltitudeFactor = Math.max(0.0D, Math.min(1.0D, Config.aerodynamicLowAltitudeStrengthFloor));
        double altitudeFactor;
        if (y >= fullStrengthY) {
            altitudeFactor = 1.0D;
        } else {
            double t = (y - (fullStrengthY - rampSpan)) / rampSpan;
            t = Math.max(0.0D, Math.min(1.0D, t));
            altitudeFactor = minimumAltitudeFactor + (1.0D - minimumAltitudeFactor) * t;
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
        double glidePitchRange = Math.max(1.0D, Config.aerodynamicGlidePitchRange);
        double pitchFactor = 1.0D - Math.min(1.0D, pitchAbs / glidePitchRange);
        pitchFactor = pitchFactor * pitchFactor;

        Vec3 horizAfter = new Vec3(velocityAfter.x, 0.0D, velocityAfter.z);
        double horizSpeedAfter = horizAfter.length();
        double totalSpeedAfter = velocityAfter.length();
        double motionHorizFactor = totalSpeedAfter <= 1.0E-6D ? 0.0D : (horizSpeedAfter / totalSpeedAfter);
        motionHorizFactor = motionHorizFactor * motionHorizFactor;

        double glideFactor = pitchFactor * motionHorizFactor;
        double upwardPitchStart = Math.max(0.0D, Config.aerodynamicUpwardPitchStart);
        double upwardPitchRange = Math.max(1.0D, Config.aerodynamicUpwardPitchRange);
        double upwardPitchFactor = -self.getXRot() > upwardPitchStart ? Math.min(1.0D, (-self.getXRot() - upwardPitchStart) / upwardPitchRange) : 0.0D;
        double upwardMotionFullSpeed = Math.max(0.01D, Config.aerodynamicUpwardMotionFullSpeed);
        double upwardMotionFactor = velocityAfter.y > 0.0D ? Math.min(1.0D, velocityAfter.y / upwardMotionFullSpeed) : 0.0D;
        double upwardEfficiencyFactor = Math.max(upwardPitchFactor, upwardMotionFactor);
        double climbAssist = upwardEfficiencyFactor * Math.max(0.0D, Config.aerodynamicClimbAssistScale);
        double glideScaledReduction = effectiveDragReduction * Math.min(1.0D, glideFactor + (1.0D - glideFactor) * climbAssist);

        Vec3 horizBefore = new Vec3(velocityBefore.x, 0.0D, velocityBefore.z);
        double horizSpeedBefore = horizBefore.length();
        double turnFactor = 0.0D;
        if (horizSpeedBefore > 1.0E-6D && horizSpeedAfter > 1.0E-6D) {
            double turnDot = horizBefore.dot(horizAfter) / (horizSpeedBefore * horizSpeedAfter);
            turnDot = Math.max(-1.0D, Math.min(1.0D, turnDot));
            double turnResponse = Math.max(0.01D, Config.aerodynamicTurnResponse);
            turnFactor = Math.min(1.0D, (1.0D - turnDot) / turnResponse);
        }
        double lostHorizSpeed = Math.max(0.0D, horizSpeedBefore - horizSpeedAfter);

        Vec3 horizDir = horizSpeedAfter <= 1.0E-6D ? new Vec3(0.0D, 0.0D, 0.0D) : horizAfter.scale(1.0D / horizSpeedAfter);
        double turnRestoreMultiplier = 1.0D + turnFactor * Math.max(0.0D, Config.aerodynamicTurnRestoreBonus);
        double restore = lostHorizSpeed * Math.min(1.0D, glideScaledReduction * turnRestoreMultiplier);

        Vec3 adjustedHoriz = horizAfter.add(horizDir.scale(restore));
        double adjustedX = adjustedHoriz.x;
        double adjustedZ = adjustedHoriz.z;

        double adjustedY = velocityAfter.y;
        if (adjustedY < 0.0D) {
            double maxSinkReduction = Math.max(0.0D, Math.min(1.0D, Config.aerodynamicMaxSinkReduction));
            double sinkMinSpeed = Math.max(0.0D, Config.aerodynamicSinkReductionMinHorizontalSpeed);
            double sinkFullSpeed = Math.max(sinkMinSpeed + 0.01D, Config.aerodynamicSinkReductionFullHorizontalSpeed);
            double sinkSpeedFactor = Math.max(0.0D, Math.min(1.0D, (horizSpeedAfter - sinkMinSpeed) / (sinkFullSpeed - sinkMinSpeed)));
            double sinkReduction = Math.min(maxSinkReduction, glideScaledReduction * Math.max(0.0D, Config.aerodynamicSinkReductionScale)) * sinkSpeedFactor;
            adjustedY *= 1.0D - sinkReduction;
        } else if (adjustedY > 0.0D && velocityBefore.y > adjustedY) {
            double lostUpwardSpeed = velocityBefore.y - adjustedY;
            double climbRestoreCap = Math.max(0.0D, Math.min(1.0D, Config.aerodynamicClimbRestoreCap));
            double climbRestore = lostUpwardSpeed * Math.min(climbRestoreCap, effectiveDragReduction * upwardEfficiencyFactor * Math.max(0.0D, Config.aerodynamicClimbRestoreScale));
            adjustedY += climbRestore;
        }

        Vec3 finalVelocity = new Vec3(adjustedX, adjustedY, adjustedZ);
        double maxMps = Config.aerodynamicMaxFlightSpeed * altitudeFactor;
        if (maxMps > 0.0D) {
            double maxBlocksPerTick = Math.max(velocityAfter.length(), maxMps / 20.0D);
            double len = finalVelocity.length();
            if (len > maxBlocksPerTick && len > 1.0E-6D) {
                finalVelocity = finalVelocity.scale(maxBlocksPerTick / len);
            }
        }

        self.setDeltaMovement(finalVelocity);

        bitsandbalance$velocityBeforeTravel = null;
    }
}
