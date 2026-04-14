package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class containing common helper methods used across mechanics modules.
 * 
 * This centralizes duplicate code patterns to reduce maintenance overhead
 * and ensure consistent behavior across all mechanics features.
 */
public final class MechanicsUtils {
    
    private MechanicsUtils() {
        // Utility class - no instantiation
    }
    
    // ========================================
    // COMMON GUARD CLAUSES
    // ========================================
    
    /**
     * Checks if we should skip processing due to client-side execution.
     * @param entity The entity to check
     * @return true if we should skip (client-side), false if we should continue (server-side)
     */
    public static boolean isClientSide(Entity entity) {
        return entity.level().isClientSide();
    }
    
    /**
     * Checks if we should skip processing due to client-side execution.
     * @param level The level to check
     * @return true if we should skip (client-side), false if we should continue (server-side)
     */
    public static boolean isClientSide(Level level) {
        return level.isClientSide();
    }
    
    /**
     * Safely casts an entity to ServerPlayer, returning null if not possible.
     * @param entity The entity to cast
     * @return ServerPlayer instance or null
     */
    public static ServerPlayer asServerPlayer(Entity entity) {
        return entity instanceof ServerPlayer sp ? sp : null;
    }
    
    /**
     * Checks if an entity is a valid server player.
     * @param entity The entity to check
     * @return true if entity is a server player, false otherwise
     */
    public static boolean isServerPlayer(Entity entity) {
        return entity instanceof ServerPlayer;
    }
    
    // ========================================
    // POSITION AND DISTANCE UTILITIES
    // ========================================
    
    /**
     * Finds a safe standing position near the given location.
     * Searches in expanding rings around the center position.
     * 
     * @param level The server level
     * @param near The target position to search near
     * @param maxRadius Maximum search radius (default: 3)
     * @return Safe position or null if none found
     */
    public static Vec3 findSafeStandingPosition(ServerLevel level, Vec3 near, int maxRadius) {
        try {
            BlockPos center = BlockPos.containing(near);
            for (int r = 0; r <= maxRadius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // Only check perimeter
                        BlockPos pos = center.offset(dx, 0, dz);
                        if (!level.isLoaded(pos)) continue;
                        
                        // Find ground level
                        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
                        BlockPos above = ground.above();
                        BlockPos above2 = above.above();
                        
                        try {
                            BlockState groundState = level.getBlockState(ground);
                            BlockState aboveState = level.getBlockState(above);
                            BlockState above2State = level.getBlockState(above2);
                            
                            // Check if ground is solid and air above
                            if (groundState.isFaceSturdy(level, ground, Direction.UP) &&
                                aboveState.isAir() && above2State.isAir()) {
                                return new Vec3(above.getX() + 0.5, above.getY(), above.getZ() + 0.5);
                            }
                        } catch (Throwable ignored) {
                            // Continue searching if this position fails
                        }
                    }
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
    
    /**
     * Finds a safe standing position near the given location with default radius.
     */
    public static Vec3 findSafeStandingPosition(ServerLevel level, Vec3 near) {
        return findSafeStandingPosition(level, near, 3);
    }
    
    /**
     * Calculates horizontal distance between two positions (ignoring Y).
     */
    public static double horizontalDistanceSqr(Vec3 pos1, Vec3 pos2) {
        double dx = pos1.x - pos2.x;
        double dz = pos1.z - pos2.z;
        return dx * dx + dz * dz;
    }
    
    /**
     * Calculates horizontal distance between two positions (ignoring Y).
     */
    public static double horizontalDistance(Vec3 pos1, Vec3 pos2) {
        return Math.sqrt(horizontalDistanceSqr(pos1, pos2));
    }
    
    // ========================================
    // SOUND UTILITIES
    // ========================================
    
    /**
     * Plays a sound effect at an entity's position.
     * @param entity The entity at whose position to play the sound
     * @param sound The sound to play
     * @param source The sound source category
     * @param volume Volume (1.0 = normal)
     * @param pitch Pitch (1.0 = normal)
     */
    public static void playSound(Entity entity, SoundEvent sound, SoundSource source, float volume, float pitch) {
        try {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, source, volume, pitch);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Plays a sound effect at a specific position.
     */
    public static void playSound(Level level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        try {
            level.playSound(null, pos.x, pos.y, pos.z, sound, source, volume, pitch);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
    
    /**
     * Plays a sound effect at an entity's position with default volume and pitch.
     */
    public static void playSound(Entity entity, SoundEvent sound, SoundSource source) {
        playSound(entity, sound, source, 1.0f, 1.0f);
    }
    
    // ========================================
    // ENTITY UTILITIES
    // ========================================
    
    /**
     * Safely checks if an entity is alive and valid.
     */
    public static boolean isEntityValid(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }
    
    /**
     * Safely gets an entity's position, returning Vec3.ZERO if invalid.
     */
    public static Vec3 getEntityPosition(Entity entity) {
        try {
            return entity != null ? entity.position() : Vec3.ZERO;
        } catch (Throwable ignored) {
            return Vec3.ZERO;
        }
    }
    
    // ========================================
    // CONFIGURATION UTILITIES
    // ========================================
    
    /**
     * Helper for common config-based early returns.
     * @param configEnabled Whether the feature is enabled in config
     * @return true if we should skip processing, false if we should continue
     */
    public static boolean shouldSkip(boolean configEnabled) {
        return !configEnabled;
    }
    
    /**
     * Combined check for config and client-side.
     * @param configEnabled Whether the feature is enabled in config
     * @param entity Entity to check for client-side
     * @return true if we should skip processing
     */
    public static boolean shouldSkip(boolean configEnabled, Entity entity) {
        return !configEnabled || isClientSide(entity);
    }
    
    /**
     * Combined check for config, client-side, and server player.
     * @param configEnabled Whether the feature is enabled in config
     * @param entity Entity to check
     * @return ServerPlayer if all checks pass, null if we should skip
     */
    public static ServerPlayer validateServerPlayer(boolean configEnabled, Entity entity) {
        if (!configEnabled || isClientSide(entity)) return null;
        return asServerPlayer(entity);
    }
}
