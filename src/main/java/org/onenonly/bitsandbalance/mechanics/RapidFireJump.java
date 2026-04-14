package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rapid Fire Jump Mechanic
 * 
 * Allows players to hold the jump key to continuously execute jumps at a configurable interval.
 * This feature only triggers when the player is within a 2-block gap or hitting their head on a ceiling
 * while jumping, making it particularly useful for quickly navigating through tight spaces where sprint jumping
 * is needed repeatedly.
 * 
 * Features:
 * - Configurable jump interval (default: 1 tick = 0.05 seconds)
 * - Only activates when player is in ceiling/gap scenarios (within 2 blocks of ceiling or hitting head)
 * - Requires ground contact for each jump (prevents mid-air spam)
 * - Debug logging for troubleshooting
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class RapidFireJump {
    
    // Track rapid fire jump state for each player
    private static final Map<UUID, RapidFireJumpState> PLAYER_RAPID_FIRE_STATES = new HashMap<>();
    
    /**
     * Represents the rapid fire jump state for a player
     */
    private static class RapidFireJumpState {
        private int ticksSinceLastJump;
        private boolean wasJumpKeyPressed;
        private boolean isOnGround;
        private long lastJumpTime;
        
        public RapidFireJumpState() {
            this.ticksSinceLastJump = 0;
            this.wasJumpKeyPressed = false;
            this.isOnGround = false;
            this.lastJumpTime = 0;
        }
        
        public void update(boolean jumpKeyPressed, boolean onGround) {
            this.wasJumpKeyPressed = jumpKeyPressed;
            this.isOnGround = onGround;
            
            if (jumpKeyPressed) {
                this.ticksSinceLastJump++;
            } else {
                // Reset timer when jump key is released
                this.ticksSinceLastJump = 0;
            }
        }
        
        public boolean canRapidFireJump() {
            if (!this.wasJumpKeyPressed) return false;
            if (!this.isOnGround) return false; // Always require ground contact
            
            // Check if enough time has passed since last jump
            return this.ticksSinceLastJump >= Config.rapidFireJumpInterval;
        }
        
        public void executeJump() {
            this.ticksSinceLastJump = 0;
            this.lastJumpTime = System.currentTimeMillis();
        }
        
        public void reset() {
            this.ticksSinceLastJump = 0;
            this.wasJumpKeyPressed = false;
            this.isOnGround = false;
            this.lastJumpTime = 0;
        }
    }
    
    /**
     * Check if a player is hitting their head on a ceiling or is within a 2-block gap
     */
    private static boolean isInCeilingOrGapScenario(Player player) {
        Level level = player.level();
        if (level == null) return false;
        
        // Get player's bounding box
        AABB playerBB = player.getBoundingBox();
        
        // Check if player is hitting their head on a ceiling
        if (isHittingCeiling(player, level, playerBB)) {
            if (Config.rapidFireJumpDebug) {
                System.out.println("[RapidFireJump] Player hitting ceiling");
            }
            return true;
        }
        
        // Check if player is within a 2-block gap (ceiling within 2 blocks above)
        if (isWithinTwoBlockGap(player, level, playerBB)) {
            if (Config.rapidFireJumpDebug) {
                System.out.println("[RapidFireJump] Player within 2-block gap");
            }
            return true;
        }
        
        if (Config.rapidFireJumpDebug) {
            System.out.println("[RapidFireJump] Player not in ceiling/gap scenario");
        }
        
        return false;
    }
    
    /**
     * Check if the player is currently hitting their head on a ceiling
     */
    private static boolean isHittingCeiling(Player player, Level level, AABB playerBB) {
        // Check the block directly above the player's head
        BlockPos headPos = BlockPos.containing(player.getX(), playerBB.maxY, player.getZ());
        BlockState headBlock = level.getBlockState(headPos);
        
        // If there's a solid block above the player's head, they're hitting the ceiling
        // Use more lenient collision detection
        return !headBlock.isAir() && headBlock.getCollisionShape(level, headPos).isEmpty() == false;
    }
    
    /**
     * Check if the player is within a 2-block gap (ceiling within 2 blocks above)
     */
    private static boolean isWithinTwoBlockGap(Player player, Level level, AABB playerBB) {
        // Check blocks above the player up to 2 blocks high
        double playerTop = playerBB.maxY;
        
        // Check for ceiling within 2 blocks above the player
        for (int y = 1; y <= 2; y++) {
            BlockPos ceilingPos = BlockPos.containing(player.getX(), playerTop + y, player.getZ());
            BlockState ceilingBlock = level.getBlockState(ceilingPos);
            
            // If we find a solid block within 2 blocks above, we're in a gap scenario
            // Use more lenient collision detection
            if (!ceilingBlock.isAir() && ceilingBlock.getCollisionShape(level, ceilingPos).isEmpty() == false) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a player can perform rapid fire jump
     */
    public static boolean canRapidFireJump(Player player, boolean jumpKeyPressed) {
        if (!Config.enableRapidFireJump) return false;
        
        UUID playerId = player.getUUID();
        RapidFireJumpState state = PLAYER_RAPID_FIRE_STATES.computeIfAbsent(playerId, k -> new RapidFireJumpState());
        
        // Update state with current input
        state.update(jumpKeyPressed, player.onGround());
        
        // Check if player is in a ceiling/gap scenario
        boolean inCeilingOrGap = isInCeilingOrGapScenario(player);
        
        // Only allow rapid fire jump if player is in ceiling/gap scenario
        if (!inCeilingOrGap) {
            if (Config.rapidFireJumpDebug && jumpKeyPressed) {
                System.out.println("[RapidFireJump] Player not in ceiling/gap scenario, rapid fire jump disabled");
            }
            return false;
        }
        
        boolean canJump = state.canRapidFireJump();
        
        if (Config.rapidFireJumpDebug && jumpKeyPressed) {
            System.out.println("[RapidFireJump] canRapidFireJump: player=" + player.getName().getString() + 
                ", canJump=" + canJump + ", ticksSince=" + state.ticksSinceLastJump + 
                ", onGround=" + player.onGround() + ", inCeilingOrGap=" + inCeilingOrGap);
        }
        
        return canJump;
    }
    
    /**
     * Perform a rapid fire jump by triggering the player's jump action
     */
    public static void performRapidFireJump(Player player) {
        if (!Config.enableRapidFireJump) return;
        
        UUID playerId = player.getUUID();
        RapidFireJumpState state = PLAYER_RAPID_FIRE_STATES.computeIfAbsent(playerId, k -> new RapidFireJumpState());
        
        // Trigger a normal Minecraft jump by calling the jump method
        player.jumpFromGround();
        
        // Mark that we performed a jump
        state.executeJump();
        
        if (Config.rapidFireJumpDebug) {
            System.out.println("[RapidFireJump] Executed rapid fire jump: player=" + player.getName().getString() + 
                ", onGround=" + player.onGround() + ", velocity=(" + 
                String.format("%.3f", player.getDeltaMovement().x) + ", " + 
                String.format("%.3f", player.getDeltaMovement().y) + ", " + 
                String.format("%.3f", player.getDeltaMovement().z) + ")");
        }
    }
    
    /**
     * Update rapid fire jump state for client-side players
     */
    public static void updateClientState(Player player, boolean jumpKeyPressed) {
        if (player.level().isClientSide()) {
            UUID playerId = player.getUUID();
            RapidFireJumpState state = PLAYER_RAPID_FIRE_STATES.computeIfAbsent(playerId, k -> new RapidFireJumpState());
            
            // Update state with current input and ground status
            state.update(jumpKeyPressed, player.onGround());
        }
    }
    
    /**
     * Reset rapid fire jump state for a player
     */
    public static void resetRapidFireJump(Player player) {
        RapidFireJumpState state = PLAYER_RAPID_FIRE_STATES.get(player.getUUID());
        if (state != null) {
            state.reset();
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.enableRapidFireJump) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        
        UUID playerId = player.getUUID();
        RapidFireJumpState state = PLAYER_RAPID_FIRE_STATES.computeIfAbsent(playerId, k -> new RapidFireJumpState());
        
        // We can't directly access jump key state on server, so we rely on client-side handling
        // The server-side tick is mainly for cleanup and state management
        
        // Clean up expired states (players who haven't been active for a while)
        long currentTime = System.currentTimeMillis();
        if (state.lastJumpTime > 0 && (currentTime - state.lastJumpTime) > 30000) { // 30 seconds of inactivity
            PLAYER_RAPID_FIRE_STATES.remove(playerId);
        }
    }
    
    /**
     * Clean up rapid fire jump states for players who have disconnected
     */
    public static void cleanupPlayer(UUID playerId) {
        PLAYER_RAPID_FIRE_STATES.remove(playerId);
    }
}
