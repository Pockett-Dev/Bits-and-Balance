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

import java.util.List;
import java.util.UUID;

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
    public boolean applyEffectTick(@Nonnull LivingEntity livingEntity, int amplifier) {
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
        ServerPlayer serverPlayer = entity instanceof ServerPlayer sp ? sp : null;
        List<UUID> leashedFollowerIds = serverPlayer != null && Config.enableLeashedTeleport
            ? LeashedTeleportModule.captureLeashedFollowerIds(serverPlayer)
            : List.of();
        
        // Spawn a few happy particles randomly around the player at the starting location
        spawnRandomHappyParticles(level, entity.getX(), entity.getY() + 1, entity.getZ());
        playTeleportSound(level, entity.getX(), entity.getY() + 1, entity.getZ());
        
        // Find the surface position above the entity
        BlockPos surfacePos = findSurfacePosition(level, currentPos);
        
        if (surfacePos != null) {
            // Teleport to surface with a small offset to avoid suffocation
            entity.teleportTo(surfacePos.getX() + 0.5, surfacePos.getY() + 1, surfacePos.getZ() + 0.5);
            
            // Handle leashed followers immediately after teleportation so potion teleports do not visibly lag.
            if (serverPlayer != null && Config.enableLeashedTeleport && !leashedFollowerIds.isEmpty()) {
                LeashedTeleportModule.teleportCapturedFollowers(
                    serverPlayer,
                    leashedFollowerIds,
                    new Vec3(surfacePos.getX() + 0.5D, surfacePos.getY() + 1.0D, surfacePos.getZ() + 0.5D)
                );
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
    
    private BlockPos findSurfacePosition(Level level, BlockPos startPos) {
        // Start from the player's X,Z coordinates but go to world height
        int x = startPos.getX();
        int z = startPos.getZ();
        
        // Start from the top of the world and work down to find the first solid block
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState blockState = level.getBlockState(checkPos);
            BlockState aboveState = level.getBlockState(checkPos.above());
            
            // Found a solid block with air or non-solid block above it
            if (!blockState.isAir() && blockState.isSolidRender(level, checkPos) &&
                (aboveState.isAir() || !aboveState.isSolidRender(level, checkPos.above()))) {
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
