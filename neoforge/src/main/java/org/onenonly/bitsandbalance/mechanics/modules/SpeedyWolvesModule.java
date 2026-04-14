package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speedy Wolves Module
 * 
 * Provides speed boosts to tamed wolves based on their owner's movement speed.
 * The faster the player moves, the faster their wolves become to help them keep up.
 * 
 * Features:
 * - Speed boost scales with player's horizontal movement speed
 * - Only affects wolves beyond a configurable distance threshold
 * - Uses transient attribute modifiers (don't persist to save files)
 * - Configurable speed multiplier and distance threshold
 * - Server-side motion sampling for accurate speed calculation
 * - Automatic speed boost removal when conditions aren't met
 * 
 * How it works:
 * - Tracks player movement over time using position sampling
 * - Calculates horizontal speed and compares to walk/sprint thresholds
 * - Applies proportional speed boost to distant tamed wolves
 * - Updates every 5 ticks for responsive but efficient performance
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class SpeedyWolvesModule implements FeatureModule {
    
    // Fixed ID for the speed modifier to avoid stacking
    private static final Identifier SPEEDY_WOLF_MOD_ID =
        Identifier.parse(BitsAndBalance.MODID + ":speedy_wolf_speed");
    
    // Server-side motion sampling per player to derive reliable horizontal speed
    private static final Map<UUID, PlayerMotionSample> LAST_PLAYER_MOTION = new HashMap<>();
    
    // Speed constants (in blocks per tick)
    private static final double WALK_SPEED = 0.22; // ~4.4 blocks/s
    private static final double SPRINT_SPEED = 0.28; // ~5.6 blocks/s
    private static final double WOLF_SEARCH_RADIUS = 16.0;
    private static final int UPDATE_INTERVAL = 5; // ticks
    
    @Override
    public String getFeatureName() {
        return "Speedy Wolves";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.enableSpeedyWolves;
    }
    
    @Override
    public int getInitializationPriority() {
        return 600; // Lower priority - pet enhancement feature
    }
    
    /**
     * Main speedy wolves logic - runs every 5 ticks for responsive performance
     */
    @SubscribeEvent
    public static void onSpeedyWolves(PlayerTickEvent.Post event) {
        if (!Config.enableSpeedyWolves) return;
        
        var entity = event.getEntity();
        if (!(entity instanceof ServerPlayer sp)) return;
        if (sp.tickCount % UPDATE_INTERVAL != 0) return; // Update every 5 ticks
        
        try {
            updatePlayerMotionSample(sp);
            double horizSpeed = calculateHorizontalSpeed(sp);
            double speedBoostAmount = calculateSpeedBoostAmount(horizSpeed);
            applySpeedBoostToNearbyWolves(sp, speedBoostAmount);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Updates the motion sample for the player to track movement over time
     */
    private static void updatePlayerMotionSample(ServerPlayer sp) {
        try {
            UUID id = sp.getUUID();
            var prev = LAST_PLAYER_MOTION.get(id);
            if (prev == null) {
                LAST_PLAYER_MOTION.put(id, new PlayerMotionSample(sp.getX(), sp.getZ(), sp.tickCount));
            }
        } catch (Throwable ignored) {}
    }
    
    /**
     * Calculates the player's horizontal speed based on motion sampling
     */
    private static double calculateHorizontalSpeed(ServerPlayer sp) {
        double horizSpeed = 0.0;
        
        try {
            var sample = LAST_PLAYER_MOTION.get(sp.getUUID());
            if (sample != null) {
                int ticksDelta = sp.tickCount - sample.tick();
                if (ticksDelta > 0) {
                    double dx = sp.getX() - sample.x();
                    double dz = sp.getZ() - sample.z();
                    double distanceTraveled = Math.sqrt(dx * dx + dz * dz);
                    horizSpeed = distanceTraveled / ticksDelta;
                } else {
                    // Fallback to current velocity
                    Vec3 vel = sp.getDeltaMovement();
                    horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                }
            } else {
                // No sample available, use current velocity
                Vec3 vel = sp.getDeltaMovement();
                horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            }
        } catch (Throwable t) {
            // Final fallback to current velocity
            Vec3 vel = sp.getDeltaMovement();
            horizSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        }
        
        // Update motion sample for next calculation
        try {
            LAST_PLAYER_MOTION.put(sp.getUUID(), new PlayerMotionSample(sp.getX(), sp.getZ(), sp.tickCount));
        } catch (Throwable ignored) {}
        
        return horizSpeed;
    }
    
    /**
     * Calculates the speed boost amount based on player's horizontal speed
     */
    private static double calculateSpeedBoostAmount(double horizSpeed) {
        double maxMultiplier = Config.speedyWolvesSpeedMultiplier;
        
        // Scale boost based on player speed: no boost at walk speed, max boost at sprint+ speed
        double speedRatio = Math.max(0.0, (horizSpeed - WALK_SPEED) / (SPRINT_SPEED - WALK_SPEED));
        speedRatio = Math.min(1.0, speedRatio); // Cap at 1.0
        
        return speedRatio * (maxMultiplier - 1.0); // 0.0 to (maxMultiplier - 1.0)
    }
    
    /**
     * Applies speed boost to nearby wolves based on distance and boost amount
     */
    private static void applySpeedBoostToNearbyWolves(ServerPlayer sp, double speedBoostAmount) {
        double thresholdSqr = Config.speedyWolvesDistanceThreshold * Config.speedyWolvesDistanceThreshold;
        
        AABB area = sp.getBoundingBox().inflate(WOLF_SEARCH_RADIUS);
        for (Entity e : sp.level().getEntities(sp, area)) {
            if (!(e instanceof Wolf wolf)) continue;
            if (!wolf.isTame() || !wolf.isOwnedBy(sp)) continue;
            
            // Only boost wolves that are beyond the distance threshold
            boolean shouldBoost = speedBoostAmount > 0.0D && wolf.distanceToSqr(sp) > thresholdSqr;
            
            if (shouldBoost) {
                applySpeedyWolfBoost(wolf, speedBoostAmount);
            } else {
                removeSpeedyWolfBoost(wolf);
            }
        }
    }
    
    /**
     * Applies a speed boost to a wolf using attribute modifiers
     */
    private static void applySpeedyWolfBoost(Wolf wolf, double amount) {
        try {
            AttributeInstance inst = wolf.getAttribute(Attributes.MOVEMENT_SPEED);
            if (inst == null) return;
            
            var existing = inst.getModifier(SPEEDY_WOLF_MOD_ID);
            if (existing != null) {
                // If the amount is the same, don't reapply
                if (Math.abs(existing.amount() - amount) < 1.0E-6) return;
                inst.removeModifier(SPEEDY_WOLF_MOD_ID);
            }
            
            AttributeModifier mod = new AttributeModifier(
                SPEEDY_WOLF_MOD_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

            // Match Fabric behavior: update-or-add and keep it transient.
            inst.addOrUpdateTransientModifier(mod);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Removes speed boost from a wolf
     */
    private static void removeSpeedyWolfBoost(Wolf wolf) {
        try {
            AttributeInstance inst = wolf.getAttribute(Attributes.MOVEMENT_SPEED);
            if (inst == null) return;

            inst.removeModifier(SPEEDY_WOLF_MOD_ID);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    @Override
    public void cleanup() {
        // Clear motion tracking data on shutdown
        LAST_PLAYER_MOTION.clear();
    }
    
    /**
     * Record for tracking player motion samples
     */
    private record PlayerMotionSample(double x, double z, int tick) {}
}
