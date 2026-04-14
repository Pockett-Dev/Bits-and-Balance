package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Fabric port of Tweaks: Enderdragon Egg Always
 * - Ensures the Ender Dragon always drops a dragon egg when killed, not just the first time.
 * - Only spawns an egg for respawned dragons (second kill and onward).
 * - Spawns after the full dragon death animation completes.
 * Server-side only.
 */
public class FabricEnderdragonEggAlways {

    // The End podium is always at these coordinates in the End dimension
    private static final BlockPos END_PODIUM_LOCATION = new BlockPos(0, 0, 0);

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!FabricTweaksConfig.enableEnderdragonEggAlways) return;
            if (!(entity instanceof EnderDragon dragon)) return;
            Level level = dragon.level();
            if (level.isClientSide()) return;
            if (!(level instanceof ServerLevel serverLevel)) return;

            // Check if this is the first dragon kill or a respawned dragon
            EndDragonFight dragonFight = serverLevel.getDragonFight();
            if (dragonFight == null) return;
            
            // Check if the exit portal already exists (indicates this is NOT the first kill)
            // First kill: no portal exists yet, vanilla will spawn egg
            // Respawned kill: portal already exists, we need to spawn egg
            boolean isRespawnedDragon = dragonFight.hasPreviouslyKilledDragon();

            // Schedule egg placement/particle effects after the dragon death animation completes
            // This ensures it spawns at the same time as vanilla (when exit portal appears)
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isRespawnedDragon) {
                        // Respawned dragon: spawn egg ourselves
                        serverLevel.getServer().execute(() -> spawnDragonEgg(serverLevel));
                    } else {
                        // First kill: vanilla spawns egg, we just add particles
                        serverLevel.getServer().execute(() -> addParticlesToVanillaEgg(serverLevel));
                    }
                }
            }, FabricTweaksConfig.enderdragonEggSpawnDelaySeconds * 1000L); // Convert seconds to milliseconds
        });
    }

    /**
     * Spawns a dragon egg near the End podium.
     * Always spawns an egg regardless of whether one already exists nearby.
     */
    private static void spawnDragonEgg(ServerLevel serverLevel) {
        // The End podium is always at (0, ?, 0) in the End dimension
        // Find the actual Y coordinate by searching for bedrock
        BlockPos podiumPos = findEndPodium(serverLevel);
        if (podiumPos == null) {
            // Fallback to a reasonable Y level if we can't find the podium
            podiumPos = new BlockPos(0, 64, 0);
        }
        
        // Determine spawn position based on config
        BlockPos spawnPos;
        if (FabricTweaksConfig.enderdragonEggUseRandomPlacement) {
            // Random placement within configured radius
            spawnPos = findSafeSpawnLocation(serverLevel, podiumPos);
        } else {
            // Vanilla placement: center of podium
            spawnPos = podiumPos.above();
        }
        
        if (spawnPos != null) {
            // Clear the spot if something is there
            if (!serverLevel.getBlockState(spawnPos).isAir()) {
                serverLevel.setBlock(spawnPos, Blocks.AIR.defaultBlockState(), 3);
            }
            
            // Place the dragon egg
            serverLevel.setBlock(spawnPos, Blocks.DRAGON_EGG.defaultBlockState(), 3);
            
            // Spawn dramatic purple particle effects to make the egg noticeable (if enabled)
            if (FabricTweaksConfig.enderdragonEggShowParticles) {
                spawnDragonEggParticles(serverLevel, spawnPos);
            }
        }
    }

    /**
     * Finds the vanilla-spawned dragon egg and adds particle effects to it.
     */
    private static void addParticlesToVanillaEgg(ServerLevel serverLevel) {
        // Find the End podium location
        BlockPos podiumPos = findEndPodium(serverLevel);
        if (podiumPos == null) {
            podiumPos = new BlockPos(0, 64, 0);
        }
        
        // Search for the vanilla-spawned dragon egg near the podium
        BlockPos eggPos = null;
        for (int xOffset = -5; xOffset <= 5; xOffset++) {
            for (int yOffset = -5; yOffset <= 10; yOffset++) {
                for (int zOffset = -5; zOffset <= 5; zOffset++) {
                    BlockPos checkPos = podiumPos.offset(xOffset, yOffset, zOffset);
                    if (serverLevel.getBlockState(checkPos).is(Blocks.DRAGON_EGG)) {
                        eggPos = checkPos;
                        break;
                    }
                }
                if (eggPos != null) break;
            }
            if (eggPos != null) break;
        }
        
        // If we found the egg, spawn particles at its location (if enabled)
        if (eggPos != null && FabricTweaksConfig.enderdragonEggShowParticles) {
            spawnDragonEggParticles(serverLevel, eggPos);
        }
    }

    /**
     * Spawns purple dragon particles that explode outward from the egg spawn location.
     */
    private static void spawnDragonEggParticles(ServerLevel level, BlockPos pos) {
        // Center position for particles
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        
        // Spawn a burst of portal particles (purple) exploding outward
        for (int i = 0; i < 20; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.3 + level.random.nextDouble() * 0.4;
            
            double velX = Math.cos(angle) * Math.cos(pitch) * speed;
            double velY = Math.sin(pitch) * speed;
            double velZ = Math.sin(angle) * Math.cos(pitch) * speed;
            
            // Portal particles (purple)
            level.sendParticles(ParticleTypes.PORTAL, 
                centerX, centerY, centerZ, 
                1, velX, velY, velZ, 0.0);
        }
        
        // Dragon breath particles - faster and more explosive
        for (int i = 0; i < 40; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double pitch = (level.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.5 + level.random.nextDouble() * 0.7;
            
            double velX = Math.cos(angle) * Math.cos(pitch) * speed;
            double velY = Math.sin(pitch) * speed;
            double velZ = Math.sin(angle) * Math.cos(pitch) * speed;
            
            // Dragon breath particles (explosive purple cloud)
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                centerX, centerY, centerZ,
                1, velX, velY, velZ, 0.0);
        }
    }

    /**
     * Finds a safe location to spawn the dragon egg within configured radius of the podium.
     * Looks for a solid block with air above it.
     */
    private static BlockPos findSafeSpawnLocation(ServerLevel level, BlockPos center) {
        int radius = FabricTweaksConfig.enderdragonEggSpawnRadius;
        
        // Try to find a good spot within configured radius
        for (int attempt = 0; attempt < 50; attempt++) {
            // Random offset within radius blocks horizontally
            int xOffset = level.random.nextInt(radius * 2 + 1) - radius;
            int zOffset = level.random.nextInt(radius * 2 + 1) - radius;
            
            // Search downward from a reasonable height to find solid ground
            for (int yOffset = 10; yOffset >= -10; yOffset--) {
                BlockPos testPos = center.offset(xOffset, yOffset, zOffset);
                BlockPos belowPos = testPos.below();
                
                // Check if this is a valid spawn location
                if (level.getBlockState(testPos).isAir() && 
                    level.getBlockState(belowPos).isSolid() &&
                    level.getBlockState(testPos.above()).isAir()) {
                    return testPos;
                }
            }
        }
        
        // Fallback: spawn near the podium if no good spot found
        return center.offset(5, 1, 5);
    }

    /**
     * Finds the End podium by looking for bedrock at (0, y, 0)
     */
    private static BlockPos findEndPodium(ServerLevel level) {
        // Search for the TOPMOST bedrock at the expected podium column (0, y, 0).
        // Using the first bedrock from y=0 can land inside the portal structure on some setups.
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);
        for (int y = maxY; y >= minY; y--) {
            pos.set(0, y, 0);
            if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                return pos.immutable();
            }
        }

        return null;
    }
}
