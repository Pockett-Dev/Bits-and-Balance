package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.MechanicsUtils;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

/**
 * Dismount Entities Module
 * 
 * Allows players to dismount passengers from vehicles by shift-right-clicking.
 * 
 * Features:
 * - Dismount passengers from any rideable entity
 * - Configurable whether players can be dismounted
 * - Ejection with velocity in vehicle's facing direction
 * - Sound effects for feedback
 * - Only works server-side to prevent desync issues
 * 
 * Usage: Shift + Right-click on a vehicle with passengers
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class DismountEntitiesModule implements FeatureModule {
    
    @Override
    public String getFeatureName() {
        return "Dismount Entities";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.enableDismountEntities;
    }
    
    @Override
    public int getInitializationPriority() {
        return 300; // Low priority - utility feature
    }
    
    /**
     * Handles dismounting passengers from vehicles when player shift-right-clicks
     */
    @SubscribeEvent
    public static void onDismountEntities(PlayerInteractEvent.EntityInteract event) {
        if (MechanicsUtils.shouldSkip(Config.enableDismountEntities, event.getEntity())) return;
        
        Player player = event.getEntity();
        DismountTarget target = resolveDismountTarget(event.getTarget());
        
        // Check if player is crouching
        if (!player.isShiftKeyDown()) return;
        
        if (target == null) return;

        Entity passenger = target.passenger;
        Entity vehicle = target.vehicle;
        
        // If passenger is a player and we don't allow dismounting players, skip
        if (passenger instanceof Player && !Config.allowDismountPlayers) return;
        
        // Dismount the passenger with velocity ejection in vehicle's facing direction
        try {
            dismountWithEjection(passenger, vehicle);
            
            // Cancel the interaction to prevent other behaviors
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Dismounts a passenger and ejects them with velocity in the vehicle's facing direction
     */
    private static void dismountWithEjection(Entity passenger, Entity vehicle) {
        Vec3 ejectionDirection = calculateEjectionDirection(vehicle);
        Vec3 ejectionPosition = findEjectionPosition(passenger, vehicle, ejectionDirection);

        // Stop the passenger from riding
        passenger.stopRiding();

        // Override vanilla's interactor-biased dismount placement with an explicit forward-side
        // placement that is synced immediately to clients before the boost is applied.
        placePassengerForEjection(passenger, vehicle, ejectionPosition);
        
        // Create velocity with configurable horizontal and vertical components
        // Horizontal velocity determines travel distance, vertical velocity determines arc height
        Vec3 ejectionVelocity = ejectionDirection.scale(Config.dismountHorizontalVelocity).add(0, Config.dismountVerticalVelocity, 0);
        
        // Apply the velocity to eject the passenger in vehicle's facing direction
        passenger.setDeltaMovement(ejectionVelocity);
        
        // Play sound effects for feedback
        playSoundEffects(passenger, vehicle);
    }
    
    /**
     * Calculates the ejection direction based on the vehicle's facing direction
     */
    private static Vec3 calculateEjectionDirection(Entity vehicle) {
        float vehicleYaw = vehicle.getYRot(); // Get the vehicle's yaw rotation
        
        // Convert yaw to direction vector (yaw 0 = south, 90 = west, 180 = north, 270 = east)
        double yawRadians = Math.toRadians(vehicleYaw);
        double dirX = -Math.sin(yawRadians); // Negative sin for correct direction
        double dirZ = Math.cos(yawRadians);
        
        return new Vec3(dirX, 0, dirZ).normalize();
    }

    private static Vec3 findEjectionPosition(Entity passenger, Entity vehicle, Vec3 ejectionDirection) {
        double baseDistance = (vehicle.getBbWidth() * 0.5D) + (passenger.getBbWidth() * 0.5D) + 0.15D;
        double targetY = passenger.getY();

        double[] multipliers = new double[] { 1.0D, 0.6D, 1.4D, 2.0D };
        for (double mul : multipliers) {
            double dist = baseDistance * mul;
            double targetX = vehicle.getX() + (ejectionDirection.x * dist);
            double targetZ = vehicle.getZ() + (ejectionDirection.z * dist);

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
    
    /**
     * Plays sound effects to provide feedback for the dismount action
     */
    private static void playSoundEffects(Entity passenger, Entity vehicle) {
        SoundEvent sound = resolveSaddleEquipSound();
        vehicle.level().playSound(null, passenger.getX(), passenger.getY(), passenger.getZ(),
            sound,
            SoundSource.NEUTRAL, 1.0F, 1.0F);
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

    private static SoundEvent resolveSaddleEquipSound() {
        try {
            var equip = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath("minecraft", "item.saddle.equip"));
            if (equip.isPresent()) return equip.get();
        } catch (Throwable ignored) {
        }
        return SoundEvents.HORSE_SADDLE.value();
    }
}
