package org.onenonly.bitsandbalance.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;

import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.modules.LeashedTeleportModule;
/**
 * Resurfacing effect - teleports the affected player to the surface.
 * Works as an instant effect like Instant Health.
 */
public class ResurfacingEffect extends MobEffect {
    
    public ResurfacingEffect() {
        // Ender pearl teal color (RGB: 0, 128, 128 -> hex: 0x008080)
        super(MobEffectCategory.BENEFICIAL, 0x008080);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, @Nonnull LivingEntity livingEntity, int amplifier) {
        // This method is called for instant effects
        if (!Config.enableResurfacingPotion) {
            return false;
        }
        
        // Apply resurfacing to all living entities, not just players
        teleportToSurface(livingEntity);
        return true;
    }

    private void teleportToSurface(LivingEntity entity) {
        Level level = entity.level();
        BlockPos currentPos = entity.blockPosition();
        
        // Spawn a few happy particles randomly around the player at the starting location
        spawnRandomHappyParticles(level, entity.getX(), entity.getY() + 1, entity.getZ());
        playTeleportSound(level, entity.getX(), entity.getY() + 1, entity.getZ());
        
        // Find the surface position above the entity
        BlockPos surfacePos = findSurfacePosition(level, currentPos);
        
        if (surfacePos != null) {
            // Teleport to surface with a small offset to avoid suffocation
            entity.teleportTo(surfacePos.getX() + 0.5, surfacePos.getY() + 1, surfacePos.getZ() + 0.5);
            
            // Handle leashed followers after teleportation (with small delay to ensure player is fully teleported)
            if (entity instanceof ServerPlayer serverPlayer && Config.enableLeashedTeleport) {
                // Schedule leashed follower teleportation for next tick to ensure player is fully teleported
                var server = serverPlayer.level().getServer();
                if (server != null) {
                    final BlockPos finalSurfacePos = surfacePos;
                    server.execute(new net.minecraft.server.TickTask(1, () -> {
                        teleportLeashedFollowers(serverPlayer, finalSurfacePos);
                    }));
                }
            }
            
            // Spawn happy particles and sound at the destination (after teleportation)
            double destX = surfacePos.getX() + 0.5;
            double destY = surfacePos.getY() + 1;
            double destZ = surfacePos.getZ() + 0.5;
            
            // Schedule particles to spawn after teleportation
            if (level instanceof ServerLevel serverLevel) {
                // Spawn celebration particles at destination
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, destX, destY, destZ, 40, 0.8, 0.8, 0.8, 0.2);
                // Play teleport sound
                serverLevel.playSound(null, destX, destY, destZ, SoundEvents.ENDERMAN_TELEPORT, 
                    net.minecraft.sounds.SoundSource.AMBIENT, 0.5f, 1.0f);
            }
        }
    }
    
    /**
     * Teleports leashed followers to the destination location
     */
    private void teleportLeashedFollowers(ServerPlayer player, BlockPos destination) {
        try {
            // Find leashed mobs around the player
            AABB searchArea = player.getBoundingBox().inflate(Config.leashedTeleportScanRadius);
            java.util.List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, searchArea);
            
            java.util.List<Mob> leashedFollowers = new java.util.ArrayList<>();
            
            for (Mob mob : nearbyMobs) {
                // Check if mob is leashed to this player
                if (mob.isLeashed() && mob.getLeashHolder() == player) {
                    // Check blacklist
                    if (Config.leashedTeleportBlacklistTypes.contains(mob.getType())) {
                        continue;
                    }
                    
                    // Check max followers limit
                    if (leashedFollowers.size() >= Config.leashedTeleportMaxFollowers) {
                        break;
                    }
                    
                    leashedFollowers.add(mob);
                }
            }
            
            if (!leashedFollowers.isEmpty()) {
                // Teleport each leashed follower
                for (Mob mob : leashedFollowers) {
                    try {
                        // Find safe placement location near destination
                        Vec3 destinationPos = new Vec3(destination.getX() + 0.5, destination.getY() + 1, destination.getZ() + 0.5);
                        Vec3 safePos = LeashedTeleportModule.findSafePlacement((ServerLevel) player.level(), destinationPos, mob);
                        
                        if (safePos == null) {
                            // Last resort: place at destination
                            safePos = destinationPos;
                        }
                        
                        // Teleport the mob
                        mob.teleportTo(safePos.x, safePos.y, safePos.z);
                        
                        // Re-attach leash if configured
                        if (Config.leashedTeleportPostTeleportLeash) {
                            mob.setLeashedTo(player, true);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("[ResurfacingEffect] Failed to teleport follower: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[ResurfacingEffect] Error in leashed follower teleportation: " + e.getMessage());
        }
    }

    private BlockPos findSurfacePosition(Level level, BlockPos startPos) {
        // Start from the player's X,Z coordinates but go to world height
        int x = startPos.getX();
        int z = startPos.getZ();
        
        // Start from the top of the world and work down to find the first solid block
        for (int y = level.getMaxY() - 1; y >= level.getMinY(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState blockState = level.getBlockState(checkPos);
            BlockState aboveState = level.getBlockState(checkPos.above());
            
            // Found a solid block with air or non-solid block above it
            if (!blockState.isAir() && blockState.isSolidRender() &&
                (aboveState.isAir() || !aboveState.isSolidRender())) {
                return checkPos;
            }
        }
        
        // If no surface found, return a position at sea level
        return new BlockPos(x, 64, z);
    }

    private void spawnRandomHappyParticles(Level level, double x, double y, double z) {
        // Only spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Spawn a few happy villager particles randomly around the player (no explosion)
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 8, 1.0, 1.0, 1.0, 0.0);
        }
    }

    private void spawnHappyParticles(Level level, double x, double y, double z) {
        // Only spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Spawn happy villager particles that explode outward (for destination)
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 40, 0.8, 0.8, 0.8, 0.2);
        }
    }

    private void playTeleportSound(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play enderman teleport sound
            serverLevel.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.5f, 1.0f);
        }
    }
}
