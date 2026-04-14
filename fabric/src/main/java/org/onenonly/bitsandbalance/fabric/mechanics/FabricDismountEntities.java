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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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
            DismountTarget target = resolveDismountTarget(entity);
            if (target == null) return net.minecraft.world.InteractionResult.PASS;

            Entity passenger = target.passenger;

            if (passenger instanceof Player && !FabricMechanicsConfig.dismountAllowPlayers) {
                return net.minecraft.world.InteractionResult.PASS;
            }

            try {
                dismountWithEjection(passenger, target.vehicle, world);
                return net.minecraft.world.InteractionResult.SUCCESS;
            } catch (Throwable ignored) {
                return net.minecraft.world.InteractionResult.PASS;
            }
        });
    }

    private static void dismountWithEjection(Entity passenger, Entity vehicle, Level level) {
        Vec3 ejectionDirection = calculateEjectionDirection(vehicle);
        Vec3 ejectionPosition = findEjectionPosition(passenger, vehicle, ejectionDirection);

        passenger.stopRiding();
        placePassengerForEjection(passenger, vehicle, ejectionPosition);

        Vec3 ejectionVelocity = ejectionDirection
                .scale(FabricMechanicsConfig.dismountHorizontalVelocity)
                .add(0.0D, FabricMechanicsConfig.dismountVerticalVelocity, 0.0D);

        passenger.setDeltaMovement(ejectionVelocity);

        SoundEvent sound = resolveSaddleEquipSound();
        level.playSound(null, passenger.getX(), passenger.getY(), passenger.getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static SoundEvent resolveSaddleEquipSound() {
        try {
            var equip = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "item.saddle.equip"));
            if (equip.isPresent()) return equip.get();
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

    private static Vec3 findEjectionPosition(Entity passenger, Entity vehicle, Vec3 ejectionDirection) {
        double baseDistance = (vehicle.getBbWidth() * 0.5D) + (passenger.getBbWidth() * 0.5D) + 0.15D;
        double targetY = passenger.getY();

        double[] multipliers = new double[] { 1.0D, 0.6D, 1.4D, 2.0D };
        for (double multiplier : multipliers) {
            double distance = baseDistance * multiplier;
            double targetX = vehicle.getX() + (ejectionDirection.x * distance);
            double targetZ = vehicle.getZ() + (ejectionDirection.z * distance);

            AABB moved = passenger.getBoundingBox().move(targetX - passenger.getX(), 0.0D, targetZ - passenger.getZ());
            boolean ok;
            if (passenger.level() instanceof ServerLevel serverLevel) {
                ok = serverLevel.noCollision(passenger, moved);
            } else {
                ok = passenger.level().noCollision(moved);
            }

            if (ok) {
                return new Vec3(targetX, targetY, targetZ);
            }
        }

        return null;
    }

    private static void placePassengerForEjection(Entity passenger, Entity vehicle, Vec3 ejectionPosition) {
        if (ejectionPosition == null) return;

        passenger.teleportTo(ejectionPosition.x, ejectionPosition.y, ejectionPosition.z);
        passenger.setYRot(vehicle.getYRot());
    }

    private static DismountTarget resolveDismountTarget(Entity clickedEntity) {
        if (!clickedEntity.getPassengers().isEmpty()) {
            Entity passenger = clickedEntity.getFirstPassenger();
            if (passenger != null) {
                return new DismountTarget(clickedEntity, passenger);
            }
        }

        Entity vehicle = clickedEntity.getVehicle();
        if (vehicle != null) {
            return new DismountTarget(vehicle, clickedEntity);
        }

        return null;
    }

    private static final class DismountTarget {
        private final Entity vehicle;
        private final Entity passenger;

        private DismountTarget(Entity vehicle, Entity passenger) {
            this.vehicle = vehicle;
            this.passenger = passenger;
        }
    }
}
