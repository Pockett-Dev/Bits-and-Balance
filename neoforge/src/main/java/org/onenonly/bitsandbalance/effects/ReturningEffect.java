package org.onenonly.bitsandbalance.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Returning effect - teleports the affected player to their latest death location.
 * Works as an instant effect like Instant Health.
 * Color: #034150 (dark teal)
 */
public class ReturningEffect extends MobEffect {
    
    private static final int MAX_ATTEMPTS = 50; // Maximum attempts to find a safe location
    private static final int TELEPORT_POST_EFFECT_TICKS = 40;
    
    public ReturningEffect() {
        // Dark teal color (RGB: 3, 65, 80 -> hex: 0x034150)
        super(MobEffectCategory.NEUTRAL, 0x034150);
    }

    @Override
    public boolean isInstantenous() {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, @Nonnull LivingEntity livingEntity, int amplifier) {
        // This method is called for instant effects
        if (!Config.enableReturningPotion) {
            return false;
        }
        
        // Only apply to players (death locations are player-specific)
        if (!(livingEntity instanceof Player player)) {
            return false;
        }
        
        teleportToDeathLocation(player);
        return true;
    }

    private void teleportToDeathLocation(Player player) {
        Level level = player.level();
        
        // Get the player's last death location
        GlobalPos deathPos = player.getLastDeathLocation().orElse(null);
        
        if (deathPos == null) {
            // No death location recorded - do nothing
            return;
        }
        
        // Check if the death location is in a different dimension
        if (!deathPos.dimension().equals(level.dimension())) {
            // For simplicity, we won't handle cross-dimensional teleportation
            // Could be enhanced later to teleport to the correct dimension
            return;
        }
        
        BlockPos deathBlockPos = deathPos.pos();
        
        // Check if the death location is in or near leaves (tree death)
        boolean isTreeDeath = isNearLeaves(level, deathBlockPos);
        
        BlockPos safePos = null;
        
        if (isTreeDeath) {
            // For tree deaths, prioritize finding position above leaves first
            safePos = findPositionAboveLeaves(level, deathBlockPos);
            
            // If no position above leaves found, try highest safe position
            if (safePos == null) {
                safePos = findHighestSafePosition(level, deathBlockPos);
            }
            
            // If still no position found, try regular safe teleport location
            if (safePos == null) {
                safePos = findSafeTeleportLocation(level, deathBlockPos);
            }
        } else {
            // For non-tree deaths, use the original order
            safePos = findSafeTeleportLocation(level, deathBlockPos);
            
            if (safePos == null) {
                safePos = findHighestSafePosition(level, deathBlockPos);
            }
            
            if (safePos == null) {
                safePos = findPositionAboveLeaves(level, deathBlockPos);
            }
        }
        
        if (safePos == null) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
        double sourceX = player.getX();
            double sourceY = player.getY() + (player.getBbHeight() * 0.5);
        double sourceZ = player.getZ();

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, false));
        spawnReturningParticles(serverLevel, serverPlayer, sourceX, sourceY, sourceZ);
        playTeleportSound(serverLevel, sourceX, sourceY, sourceZ);

        finishTeleport(player, safePos.immutable());
    }

    private void finishTeleport(Player player, BlockPos safePos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (player.getVehicle() != null) {
            player.stopRiding();
        }

        player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TELEPORT_POST_EFFECT_TICKS, 0, false, true, true));
        player.removeEffect(MobEffects.INVISIBILITY);

        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
    double destX = player.getX();
    double destY = player.getY() + (player.getBbHeight() * 0.5);
    double destZ = player.getZ();

        spawnReturningParticles(serverLevel, serverPlayer, destX, destY, destZ);
        playArrivalSounds(serverLevel, destX, destY, destZ);

        if (serverPlayer != null && Config.enableLeashedTeleport) {
            var server = serverPlayer.level().getServer();
            if (server != null) {
                server.execute(new net.minecraft.server.TickTask(1, () -> teleportLeashedFollowers(serverPlayer, safePos)));
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
                        Vec3 destinationPos = new Vec3(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
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
                        System.err.println("[ReturningEffect] Failed to teleport follower: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[ReturningEffect] Error in leashed follower teleportation: " + e.getMessage());
        }
    }

    private BlockPos findSafeTeleportLocation(Level level, BlockPos startPos) {
        // First try the exact death location
        if (isSafeTeleportLocation(level, startPos)) {
            return startPos;
        }
        
        // Try locations in expanding radius around the death location
        for (int radius = 1; radius <= 10; radius++) {
            for (int attempt = 0; attempt < MAX_ATTEMPTS / 10; attempt++) {
                // Generate random coordinates within the current radius
                int offsetX = level.random.nextInt(radius * 2 + 1) - radius;
                int offsetZ = level.random.nextInt(radius * 2 + 1) - radius;
                
                int targetX = startPos.getX() + offsetX;
                int targetZ = startPos.getZ() + offsetZ;
                
                // Find a safe Y position at this X,Z coordinate
                BlockPos safePos = findSafeYPosition(level, targetX, targetZ, startPos.getY());
                
                if (safePos != null) {
                    return safePos;
                }
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
        
        // Additional check: make sure we're not in leaves (which are technically passable but annoying)
        boolean notInLeaves = !isLeafBlock(feetState) && !isLeafBlock(headState);
        
        // Check if there's solid ground below (not void) - allow for some flexibility
        boolean hasGround = !groundState.isAir() && groundState.isSolidRender();
        
        // Check if the position is not in the void
        boolean notInVoid = pos.getY() >= level.getMinY();
        
        // Additional safety: make sure we're not in lava or dangerous blocks
        boolean notInLava = !feetState.is(net.minecraft.world.level.block.Blocks.LAVA) && 
                           !headState.is(net.minecraft.world.level.block.Blocks.LAVA);
        
        return feetSafe && headSafe && notInLeaves && hasGround && notInVoid && notInLava;
    }

    private boolean isLeafBlock(BlockState state) {
        // Check if the block is a leaf block using the leaves tag
        return state.is(net.minecraft.tags.BlockTags.LEAVES);
    }
    
    private boolean isNearLeaves(Level level, BlockPos pos) {
        // Check if the position is in or near leaves (within 2 blocks)
        int checkRadius = 2;
        
        for (int offsetX = -checkRadius; offsetX <= checkRadius; offsetX++) {
            for (int offsetY = -checkRadius; offsetY <= checkRadius; offsetY++) {
                for (int offsetZ = -checkRadius; offsetZ <= checkRadius; offsetZ++) {
                    BlockPos checkPos = pos.offset(offsetX, offsetY, offsetZ);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (isLeafBlock(state)) {
                        return true; // Found leaves nearby
                    }
                }
            }
        }
        
        return false; // No leaves found nearby
    }

    private BlockPos findHighestSafePosition(Level level, BlockPos startPos) {
        // As a fallback, find the highest safe position near the death location
        // Search in a small radius to find the best position
        int searchRadius = 2; // Search within 2 blocks radius
        BlockPos bestPosition = null;
        int highestY = -1;
        
        // Search in a radius around the death location
        for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
            for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
                int x = startPos.getX() + offsetX;
                int z = startPos.getZ() + offsetZ;
                
                // Start from the top and work down to find the first safe spot
                for (int y = level.getMaxY() - 2; y >= level.getMinY(); y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (isSafeTeleportLocation(level, checkPos)) {
                        // Found a safe position - check if it's higher than our current best
                        if (y > highestY) {
                            highestY = y;
                            bestPosition = checkPos;
                        }
                        break; // Found the highest safe position at this X,Z, move to next position
                    }
                }
            }
        }
        
        // If we found a safe position, return it
        if (bestPosition != null) {
            return bestPosition;
        }
        
        // Ultimate fallback: place on solid ground with air above (even without perfect safety)
        // Search in radius for the best fallback position
        for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
            for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
                int x = startPos.getX() + offsetX;
                int z = startPos.getZ() + offsetZ;
                
                for (int y = level.getMaxY() - 2; y >= level.getMinY(); y--) {
                    BlockPos groundPos = new BlockPos(x, y - 1, z);
                    BlockPos feetPos = new BlockPos(x, y, z);
                    BlockPos headPos = new BlockPos(x, y + 1, z);
                    
                    BlockState groundState = level.getBlockState(groundPos);
                    BlockState feetState = level.getBlockState(feetPos);
                    BlockState headState = level.getBlockState(headPos);
                    
                    if (!groundState.isAir() && groundState.isSolidRender() &&
                        feetState.isAir() && headState.isAir()) {
                        // Found a fallback position - check if it's higher than our current best
                        if (y > highestY) {
                            highestY = y;
                            bestPosition = feetPos;
                        }
                        break; // Found the highest fallback position at this X,Z, move to next position
                    }
                }
            }
        }
        
        return bestPosition; // Return the best position found, or null if none found
    }

    private BlockPos findPositionAboveLeaves(Level level, BlockPos startPos) {
        // Specifically handle teleporting above leaves when death location is in a tree
        // Search in a small radius around the death location to find the actual tree top
        int searchRadius = 3; // Search within 3 blocks radius
        int highestLeafY = -1;
        BlockPos bestTreeTop = null;
        
        // Search in a radius around the death location to find the highest leaf block
        for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
            for (int offsetZ = -searchRadius; offsetZ <= searchRadius; offsetZ++) {
                int x = startPos.getX() + offsetX;
                int z = startPos.getZ() + offsetZ;
                
                // Find the highest leaf block at this X,Z coordinate
                for (int y = level.getMaxY() - 1; y >= level.getMinY(); y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (isLeafBlock(state)) {
                        // Found a leaf block - check if it's higher than our current highest
                        if (y > highestLeafY) {
                            highestLeafY = y;
                            bestTreeTop = new BlockPos(x, y, z);
                        }
                        break; // Found the highest leaf at this X,Z, move to next position
                    }
                }
            }
        }
        
        // If we found leaves, try to place the player above the highest leaf
        if (highestLeafY >= 0 && bestTreeTop != null) {
            int x = bestTreeTop.getX();
            int z = bestTreeTop.getZ();
            
            // Try to place the player above the highest leaf found
            for (int y = highestLeafY + 1; y < level.getMaxY() - 1; y++) {
                BlockPos feetPos = new BlockPos(x, y, z);
                BlockPos headPos = new BlockPos(x, y + 1, z);
                
                BlockState feetState = level.getBlockState(feetPos);
                BlockState headState = level.getBlockState(headPos);
                
                // For tree tops, use a more lenient check - just need air for feet and head
                // Don't require solid ground below since we're on top of a tree
                if (feetState.isAir() && headState.isAir()) {
                    return feetPos;
                }
            }
        }
        
        return null; // No leaves found or no safe position above them
    }

    private void spawnWarpParticles(Level level, double x, double y, double z) {
        // Only spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Spawn soul particles for a mystical/returning effect
            serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 30, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void spawnReturningParticles(ServerLevel serverLevel, ServerPlayer focusPlayer, double x, double y, double z) {
        serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);

        if (focusPlayer != null) {
            sendForcedParticles(serverLevel, focusPlayer, x, y, z);
        }

        for (ServerPlayer nearbyPlayer : serverLevel.players()) {
            if (focusPlayer != null && nearbyPlayer == focusPlayer) {
                continue;
            }
            if (nearbyPlayer.distanceToSqr(x, y, z) <= 4096.0D) {
                sendForcedParticles(serverLevel, nearbyPlayer, x, y, z);
            }
        }
    }

    private void sendForcedParticles(ServerLevel serverLevel, ServerPlayer player, double x, double y, double z) {
        serverLevel.sendParticles(player, ParticleTypes.SOUL, true, true, x, y, z, 34, 0.08, 0.12, 0.08, 0.05);
        serverLevel.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, true, x, y, z, 10, 0.05, 0.08, 0.05, 0.03);
    }


    private void playTeleportSound(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play Enderman teleport sound at the location
            serverLevel.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.5f, 1.0f);
            // Also play a soul sand sound for the mystical effect
            serverLevel.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.3f, 0.8f);
        }
    }

    private void playArrivalSounds(Level level, double x, double y, double z) {
        // Only play sounds on the server side
        if (level instanceof ServerLevel serverLevel) {
            // Play soul sand sound for the mystical effect (no enderman teleport at arrival)
            serverLevel.playSound(null, x, y, z, SoundEvents.SOUL_SAND_STEP, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.3f, 0.8f);
            // Play ambient cave sound at the death location for eerie atmosphere
            serverLevel.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE, 
                net.minecraft.sounds.SoundSource.AMBIENT, 0.4f, 0.8f);
        }
    }
}
