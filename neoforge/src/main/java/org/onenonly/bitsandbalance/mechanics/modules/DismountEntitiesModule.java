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
        Entity target = event.getTarget();
        
        // Check if player is crouching
        if (!player.isShiftKeyDown()) return;
        
        // Check if target entity has passengers
        if (target.getPassengers().isEmpty()) return;
        
        // Get the first passenger
        Entity passenger = target.getFirstPassenger();
        if (passenger == null) return;
        
        // If passenger is a player and we don't allow dismounting players, skip
        if (passenger instanceof Player && !Config.allowDismountPlayers) return;
        
        // Dismount the passenger with velocity ejection in vehicle's facing direction
        try {
            dismountWithEjection(passenger, target, player);
            
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
    private static void dismountWithEjection(Entity passenger, Entity vehicle, Player player) {
        // Stop the passenger from riding
        passenger.stopRiding();
        
        // Calculate ejection direction based on the vehicle's facing direction
        Vec3 ejectionDirection = calculateEjectionDirection(vehicle);

        // Vanilla dismount placement can prefer the interactor side (the player), which looks odd
        // when we then apply forward velocity. Reposition the passenger to the vehicle's forward
        // side so the eject is cleanly "forward".
        placePassengerForEjection(passenger, vehicle, ejectionDirection);
        
        // Create velocity with configurable horizontal and vertical components
        // Horizontal velocity determines travel distance, vertical velocity determines arc height
        Vec3 ejectionVelocity = ejectionDirection.scale(Config.dismountHorizontalVelocity).add(0, Config.dismountVerticalVelocity, 0);
        
        // Apply the velocity to eject the passenger in vehicle's facing direction
        passenger.setDeltaMovement(ejectionVelocity);
        
        // Play sound effects for feedback
        playSoundEffects(vehicle, player);
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

    private static void placePassengerForEjection(Entity passenger, Entity vehicle, Vec3 ejectionDirection) {
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
                passenger.setPos(targetX, targetY, targetZ);
                passenger.setYRot(vehicle.getYRot());
                return;
            }
        }
    }
    
    /**
     * Plays sound effects to provide feedback for the dismount action
     */
    private static void playSoundEffects(Entity vehicle, Player player) {
        SoundEvent sound = resolveSaddleUnequipSound();
        player.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
            sound,
            SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static SoundEvent resolveSaddleUnequipSound() {
        // Prefer the modern saddle remove/unequip sound if present; fall back to the classic horse saddle sound.
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
}
