package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;
import org.onenonly.bitsandbalance.fabric.config.FabricEnchantingConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enchanting: Aerodynamic
 *
 * Reduces horizontal air resistance during elytra flight based on the aerodynamic_drag attribute.
 * The attribute value is applied by the data-driven enchantment definition.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAerodynamicMixin {
    @Unique
    private Vec3 bitsandbalance$velocityBeforeTravel;

    @Inject(method = "travel", at = @At("HEAD"))
    private void bitsandbalance$captureVelocity(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!FabricEnchantingConfig.enableAerodynamic) return;
        if (!self.isFallFlying()) return;

        bitsandbalance$velocityBeforeTravel = self.getDeltaMovement();
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void bitsandbalance$applyAerodynamic(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!FabricEnchantingConfig.enableAerodynamic) return;
        if (!self.isFallFlying()) return;
        if (bitsandbalance$velocityBeforeTravel == null) return;

        // Prefer the data-driven attribute path (enchantment JSON applies aerodynamic_drag).
        double dragReduction = 0.0D;
        try {
            var holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(BitsAndBalanceFabric.AERODYNAMIC_DRAG);
            var inst = self.getAttribute(holder);
            if (inst != null) {
                dragReduction = inst.getValue();
            }
        } catch (Throwable ignored) {
        }

        // Fallback for environments where attribute modifiers from data-driven enchantments
        // are not being applied to the entity attribute map as expected.
        if (dragReduction <= 0.0D) {
            ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty() && chest.is(Items.ELYTRA)) {
                try {
                    var enchantmentLookup = self.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                    ResourceKey<Enchantment> aerodynamicKey = ResourceKey.create(
                            Registries.ENCHANTMENT,
                            Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "aerodynamic")
                    );
                    var aerodynamicHolder = enchantmentLookup.get(aerodynamicKey).orElse(null);
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

        dragReduction = Math.max(0.0D, Math.min(1.0D, dragReduction));

        double y = self.getY();
        double minY = (double) FabricEnchantingConfig.aerodynamicMinAltitude;
        double maxY = (double) FabricEnchantingConfig.aerodynamicMaxAltitude;
        double cloudY = FabricEnchantingConfig.aerodynamicCloudLevel;

        double altitudeFactor;
        if (y <= minY) {
            altitudeFactor = 0.0D;
        } else if (y >= maxY) {
            altitudeFactor = 1.0D;
        } else {
            double denom = Math.max(1.0D, maxY - minY);
            double t = (y - minY) / denom;

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

        double baseScale = FabricEnchantingConfig.aerodynamicBaseDragReduction / 0.5D;
        double effectiveDragReduction = dragReduction * baseScale * altitudeFactor * FabricEnchantingConfig.aerodynamicSpeedMultiplier;
        effectiveDragReduction = Math.max(0.0D, Math.min(1.0D, effectiveDragReduction));

        Vec3 velocityAfter = self.getDeltaMovement();
        Vec3 velocityBefore = bitsandbalance$velocityBeforeTravel;

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

        Vec3 horizBefore = new Vec3(velocityBefore.x, 0.0D, velocityBefore.z);
        double horizSpeedBefore = horizBefore.length();
        double lostHorizSpeed = Math.max(0.0D, horizSpeedBefore - horizSpeedAfter);

        Vec3 horizDir = horizSpeedAfter <= 1.0E-6D ? new Vec3(0.0D, 0.0D, 0.0D) : horizAfter.scale(1.0D / horizSpeedAfter);
        double restore = lostHorizSpeed * glideScaledReduction;

        double baseBoostPerTick = 0.10D;
        double boost = baseBoostPerTick * glideScaledReduction;
        boost = Math.min(boost, 0.22D);

        Vec3 boostedHoriz = horizAfter.add(horizDir.scale(restore + boost));
        Vec3 finalVelocity = new Vec3(boostedHoriz.x, velocityAfter.y, boostedHoriz.z);

        double maxMps = FabricEnchantingConfig.aerodynamicMaxFlightSpeed;
        if (maxMps > 0.0D) {
            double maxBlocksPerTick = maxMps / 20.0D;
            double len = finalVelocity.length();
            if (len > maxBlocksPerTick && len > 1.0E-6D) {
                finalVelocity = finalVelocity.scale(maxBlocksPerTick / len);
            }
        }

        self.setDeltaMovement(finalVelocity);

        bitsandbalance$velocityBeforeTravel = null;
    }
}
