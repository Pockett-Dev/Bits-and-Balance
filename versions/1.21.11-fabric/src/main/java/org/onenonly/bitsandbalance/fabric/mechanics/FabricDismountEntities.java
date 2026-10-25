package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

/**
 * Fabric port of NeoForge's Dismount Entities mechanic.
 *
 * Usage: Shift + Right-click on a vehicle with passengers.
 */
public final class FabricDismountEntities {
    private FabricDismountEntities() {
    }

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!FabricMechanicsConfig.enableDismountEntities) return net.minecraft.world.InteractionResult.PASS;
            if (world.isClientSide()) return net.minecraft.world.InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return net.minecraft.world.InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
            if (entity.getPassengers().isEmpty()) return net.minecraft.world.InteractionResult.PASS;

            Entity passenger = entity.getFirstPassenger();
            if (passenger == null) return net.minecraft.world.InteractionResult.PASS;

            if (passenger instanceof Player && !FabricMechanicsConfig.dismountAllowPlayers) {
                return net.minecraft.world.InteractionResult.PASS;
            }

            try {
                dismountWithEjection(passenger, entity, world);
                return net.minecraft.world.InteractionResult.SUCCESS;
            } catch (Throwable ignored) {
                return net.minecraft.world.InteractionResult.PASS;
            }
        });
    }

    private static void dismountWithEjection(Entity passenger, Entity vehicle, Level level) {
        passenger.stopRiding();

        Vec3 ejectionDirection = calculateEjectionDirection(vehicle);
        Vec3 ejectionVelocity = ejectionDirection
                .scale(FabricMechanicsConfig.dismountHorizontalVelocity)
                .add(0.0D, FabricMechanicsConfig.dismountVerticalVelocity, 0.0D);

        passenger.setDeltaMovement(ejectionVelocity);

        SoundEvent sound = resolveSaddleUnequipSound();
        level.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static SoundEvent resolveSaddleUnequipSound() {
        try {
            var remove = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "item.saddle.remove"));
            if (remove.isPresent()) return remove.get();
        } catch (Throwable ignored) {
        }
        try {
            var unequip = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "item.saddle.unequip"));
            if (unequip.isPresent()) return unequip.get();
        } catch (Throwable ignored) {
        }
        return SoundEvents.HORSE_SADDLE.value();
    }

    private static Vec3 calculateEjectionDirection(Entity vehicle) {
        float vehicleYaw = vehicle.getYRot();
        double yawRadians = Math.toRadians(vehicleYaw);
        double dirX = -Math.sin(yawRadians);
        double dirZ = Math.cos(yawRadians);
        return new Vec3(dirX, 0.0D, dirZ).normalize();
    }
}
