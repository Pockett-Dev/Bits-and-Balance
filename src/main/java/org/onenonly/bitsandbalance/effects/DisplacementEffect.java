package org.onenonly.bitsandbalance.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;

import java.util.Random;

import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.modules.LeashedTeleportModule;
/**
 * Displacement effect - randomly teleports the affected player within a 100-block radius.
 * Works as an instant effect like Instant Health.
 */
public class DisplacementEffect extends MobEffect {
    
    private static final Random RANDOM = new Random();
    private static final int TELEPORT_RADIUS = 100;
    private static final int MAX_ATTEMPTS = 50; // Maximum attempts to find a safe location
    
    public DisplacementEffect() {
        // Ender purple color (RGB: 128, 0, 128 -> hex: 0x800080)
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, @Nonnull LivingEntity livingEntity, int amplifier) {
        // This method is called for instant effects
        if (!Config.enableDisplacementPotion) {
            return false;
        }
        
        // Apply displacement to all living entities, not just players
        teleportRandomly(livingEntity);
        return true;
    }

    private void teleportRandomly(LivingEntity entity) {
        Level level = entity.level();
        BlockPos currentPos = entity.blockPosition();
        
        // Try to find a safe teleport location within the radius
        BlockPos safePos = findSafeTeleportLocation(level, currentPos);
        
        if (safePos != null) {
            // Make entity invisible before teleportation
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false));
            
            // Spawn warp particles and sound at the starting location
            spawnWarpParticles(level, currentPos.getX() + 0.5, currentPos.getY() + 1, currentPos.getZ() + 0.5);
            playTeleportSound(level, currentPos.getX() + 0.5, currentPos.getY() + 1, currentPos.getZ() + 0.5);
            
            // Teleport to the safe location
            entity.teleportTo(safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5);
            
            // Handle leashed followers after teleportation (with small delay to ensure player is fully teleported)
            if (entity instanceof ServerPlayer serverPlayer && Config.enableLeashedTeleport) {
                // Schedule leashed follower teleportation for next tick to ensure player is fully teleported
                var server = serverPlayer.level().getServer();
                if (server != null) {
                    final BlockPos finalSafePos = safePos;
                    server.execute(new net.minecraft.server.TickTask(1, () -> {
                        teleportLeashedFollowers(serverPlayer, finalSafePos);
                    }));
                }
            }
            
            // Remove invisibility immediately after teleportation
            entity.removeEffect(MobEffects.INVISIBILITY);
            
            // Spawn warp particles and sound at the destination
            spawnWarpParticles(level, safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5);
            playTeleportSound(level, safePos.getX() + 0.5, safePos.getY() + 1, safePos.getZ() + 0.5);
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
                        Vec3 safePos = LeashedTeleportModule.findSafePlacement((net.minecraft.server.level.ServerLevel) player.level(), destinationPos, mob);
                        
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
                        System.err.println("[DisplacementEffect] Failed to teleport follower: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[DisplacementEffect] Error in leashed follower teleportation: " + e.getMessage());
        }
    }

    private BlockPos findSafeTeleportLocation(Level level, BlockPos startPos) {
        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Generate random coordinates within the radius
            int offsetX = RANDOM.nextInt(TELEPORT_RADIUS * 2 + 1) - TELEPORT_RADIUS;
            int offsetZ = RANDOM.nextInt(TELEPORT_RADIUS * 2 + 1) - TELEPORT_RADIUS;
            
            int targetX = startX + offsetX;
            int targetZ = startZ + offsetZ;
            
            // Find a safe Y position at this X,Z coordinate
            BlockPos safePos = findSafeYPosition(level, targetX, targetZ, startY);
            
            if (safePos != null) {
                return safePos;
            }
        }
        
        // If no safe location found after max attempts, return null
        return null;
    }

    private BlockPos findSafeYPosition(Level level, int x, int z, int preferredY) {
        // Start from the preferred Y position and search both up and down
        int[] searchOrder = new int[level.getMaxY() - level.getMinY()];
        int index = 0;
        
        // Add positions around the preferred Y first
        for (int offset = 0; offset < 50; offset++) {
            int yUp = preferredY + offset;
            int yDown = preferredY - offset;
            
            if (yUp >= level.getMinY() && yUp < level.getMaxY()) {
                searchOrder[index++] = yUp;
            }
            if (yDown >= level.getMinY() && yDown < level.getMaxY() && offset > 0) {
                searchOrder[index++] = yDown;
            }
        }
        
        // Check each Y position for safety
        for (int i = 0; i < index; i++) {
            int y = searchOrder[i];
            BlockPos checkPos = new BlockPos(x, y, z);
            
            if (isSafeTeleportLocation(level, checkPos)) {
                return checkPos;
            }
        }
        
        return null;
    }

    private boolean isSafeTeleportLocation(Level level, BlockPos pos) {
        BlockPos feetPos = pos;
        BlockPos headPos = pos.above();
        BlockPos groundPos = pos.below();
        
        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);
        BlockState groundState = level.getBlockState(groundPos);
        
        // Check if the feet and head positions are safe (air or non-solid)
        boolean feetSafe = feetState.isAir() || !feetState.isSolidRender();
        boolean headSafe = headState.isAir() || !headState.isSolidRender();
        
        // Check if there's solid ground below (not void)
        boolean hasGround = !groundState.isAir() && groundState.isSolidRender();
        
        // Check if the position is not in the void
        boolean notInVoid = pos.getY() >= level.getMinY();
        
        return feetSafe && headSafe && hasGround && notInVoid;
    }

    private void spawnWarpParticles(Level level, double x, double y, double z) {
        // Only spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Spawn portal particles similar to Enderman teleportation
            serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 30, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void playTeleportSound(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play Enderman teleport sound at the location
            serverLevel.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.5f, 1.0f);
        }
    }
}
