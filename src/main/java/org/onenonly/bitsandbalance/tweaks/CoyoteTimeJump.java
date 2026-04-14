package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coyote Time Jump Tweak
 * 
 * Allows players to jump for a short time after falling off a block without jumping first.
 * This is a common quality-of-life feature in platformer games that makes movement feel
 * more forgiving and responsive.
 * 
 * The coyote time window starts when a player falls off a block while on the ground
 * and lasts for a configurable duration (default 2 seconds).
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class CoyoteTimeJump {
    
    // Track coyote time state for each player.
    // Client and server run on different threads in singleplayer (integrated server),
    // so we keep states separated per side to avoid concurrent mutation.
    private static final Map<UUID, CoyoteTimeState> CLIENT_PLAYER_COYOTE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, CoyoteTimeState> SERVER_PLAYER_COYOTE_STATES = new ConcurrentHashMap<>();

    private static Map<UUID, CoyoteTimeState> getStateMap(boolean isClientSide) {
        return isClientSide ? CLIENT_PLAYER_COYOTE_STATES : SERVER_PLAYER_COYOTE_STATES;
    }

    private static CoyoteTimeState getState(Player player) {
        return getStateMap(player.level().isClientSide()).get(player.getUUID());
    }

    private static CoyoteTimeState getOrCreateState(Player player) {
        UUID playerId = player.getUUID();
        return getStateMap(player.level().isClientSide()).computeIfAbsent(playerId, k -> new CoyoteTimeState());
    }
    
    /**
     * Represents the coyote time state for a player
     */
    private static class CoyoteTimeState {
        private long coyoteTimeStart;
        private boolean wasOnGround;
        private boolean hasUsedCoyoteTime;
        private boolean hasJumped; // Track if player has jumped since last being on ground
        private Vec3 lastPosition; // Track last position to detect falling
        private double lastVelocityY; // Track last Y velocity to detect jumping vs falling
        private long lastGroundTime; // Track when player last touched ground
        private double fallStartY; // Track Y position when coyote time started
        
        public CoyoteTimeState() {
            this.coyoteTimeStart = 0;
            this.wasOnGround = false;
            this.hasUsedCoyoteTime = false;
            this.hasJumped = false;
            this.lastPosition = null;
            this.lastVelocityY = 0.0;
            this.lastGroundTime = 0;
            this.fallStartY = 0.0;
        }
        
        public void updateGroundState(boolean onGround, Vec3 position, double velocityY, Player player) {
            if (player.isInWater() || player.isInLava()) {
                // Never allow coyote-time jumps while touching liquids.
                reset();
                return;
            }

            // Detect if player has jumped (positive velocity when leaving ground)
            if (this.wasOnGround && !onGround && velocityY > 0) {
                this.hasJumped = true;
                if (Config.coyoteTimeDebug) {
                    System.out.println("[CoyoteTime] Player jumped, no coyote time available");
                }
            }
            
            if (onGround) {
                this.wasOnGround = true;
                this.hasJumped = false; // Reset jump flag when on ground
                this.coyoteTimeStart = 0; // Reset coyote time when on ground
                this.lastPosition = position; // Update last position
                this.lastVelocityY = velocityY; // Update velocity
                
                // Only reset hasUsedCoyoteTime after being on ground for 200ms
                long currentTime = System.currentTimeMillis();
                if (this.lastGroundTime == 0) {
                    // First time touching ground, start timer
                    this.lastGroundTime = currentTime;
                } else if ((currentTime - this.lastGroundTime) >= 200) {
                    // Been on ground for 200ms, reset the flag
                    this.hasUsedCoyoteTime = false;
                }
            } else {
                // Not on ground, reset ground timer
                this.lastGroundTime = 0;
                
                if (this.wasOnGround && !this.hasUsedCoyoteTime && this.coyoteTimeStart == 0 && !this.hasJumped) {
                    // Only start coyote time if:
                    // 1. Player was on ground and is now off ground
                    // 2. Player is falling (velocity.y <= 0)
                    // 3. Player has NOT jumped (hasJumped = false)
                    // 4. Player is not flying, riding, etc.
                    if (velocityY <= 0 && !player.isFallFlying() && !player.isPassenger()) {
                        // Player just fell off a block and is descending, start coyote time
                        this.coyoteTimeStart = System.currentTimeMillis();
                        this.fallStartY = position.y; // Record the Y position when falling started
                        this.wasOnGround = false;
                        if (Config.coyoteTimeDebug) {
                            System.out.println("[CoyoteTime] Player fell off block, starting coyote time (fallStartY=" + this.fallStartY + ")");
                        }
                    }
                }
            }
            
            // Update last position and velocity
            this.lastPosition = position;
            this.lastVelocityY = velocityY;
        }
        
        public boolean isCoyoteTimeActive() {
            if (this.hasUsedCoyoteTime) return false;
            if (this.coyoteTimeStart == 0) return false;

            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - this.coyoteTimeStart;
            long delayMs = Config.coyoteTimeDelayMs;
            long windowMs = Config.coyoteTimeWindowMs;

            return timeSinceStart >= delayMs && timeSinceStart <= windowMs;
        }
        
        public void useCoyoteTime() {
            this.hasUsedCoyoteTime = true;
            this.coyoteTimeStart = 0;
        }
        
        public void reset() {
            this.coyoteTimeStart = 0;
            this.wasOnGround = false;
            this.hasUsedCoyoteTime = false;
            this.hasJumped = false;
            this.lastPosition = null;
            this.lastVelocityY = 0.0;
            this.lastGroundTime = 0;
            this.fallStartY = 0.0;
        }
    }
    
    /**
     * Check if a player can use coyote time jump
     */
    public static boolean canUseCoyoteTime(Player player) {
        if (!Config.enableCoyoteTimeJump) return false;
        if (player.isInWater() || player.isInLava()) return false;
        
        CoyoteTimeState state = getState(player);
        if (state == null) return false;
        
        boolean canUse = state.isCoyoteTimeActive();

        if (Config.coyoteTimeDebug) {
            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - state.coyoteTimeStart;
            System.out.println("[CoyoteTime] canUseCoyoteTime: hasUsed=" + state.hasUsedCoyoteTime + ", canUse=" + canUse + ", timeSinceStartMs=" + timeSinceStart + ", delayMs=" + Config.coyoteTimeDelayMs + ", windowMs=" + Config.coyoteTimeWindowMs);
        }
        return canUse;
    }
    
    /**
     * Mark that a player has used their coyote time
     */
    public static void useCoyoteTime(Player player) {
        CoyoteTimeState state = getState(player);
        if (state != null) {
            if (Config.coyoteTimeDebug) {
                System.out.println("[CoyoteTime] useCoyoteTime called, setting hasUsedCoyoteTime = true");
            }
            state.useCoyoteTime();
        } else {
            if (Config.coyoteTimeDebug) {
                System.out.println("[CoyoteTime] useCoyoteTime called but no state found for player");
            }
        }
    }
    
    /**
     * Perform a coyote time jump by applying velocity directly to the player
     */
    public static void performCoyoteTimeJump(Player player) {
        if (!Config.enableCoyoteTimeJump) return;
        
        // Only perform on server side for proper synchronization
        if (player.level().isClientSide()) {
            // Send packet to server to perform the jump
            // For now, we'll handle this client-side for immediate response
            // In a more robust implementation, you'd send a packet to the server
        }
        
        // Get current velocity to preserve horizontal movement
        var currentVelocity = player.getDeltaMovement();
        
        // Calculate jump velocity based on player's current speed and effects
        double jumpVelocity = 0.5D; // Increased to 0.5D for higher coyote time jump
        
        // Apply jump boost effect
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST)) {
            jumpVelocity += 0.1D * (player.getEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST).getAmplifier() + 1);
        }
        
        // Scale horizontal velocity based on player's current speed
        double horizontalSpeed = Math.sqrt(currentVelocity.x * currentVelocity.x + currentVelocity.z * currentVelocity.z);
        
        // Scale horizontal velocity based on current speed
        // Base multiplier of 1.0, then add speed-based scaling (further increased for more responsive sprinting)
        double horizontalMultiplier = 1.0 + (horizontalSpeed * 3.2); // Increased from 3.0 to 3.2 for slightly more horizontal velocity
        
        // Cap the multiplier to prevent excessive horizontal movement
        horizontalMultiplier = Math.min(horizontalMultiplier, 3.2); // Increased cap from 3.0 to 3.2
        
        // Apply minimum multiplier to ensure horizontal movement always feels responsive
        horizontalMultiplier = Math.max(horizontalMultiplier, 1.5); // Increased minimum from 1.4 to 1.5
        
        // Scale only the horizontal velocity, keep vertical jump velocity constant
        double scaledHorizontalX = currentVelocity.x * horizontalMultiplier;
        double scaledHorizontalZ = currentVelocity.z * horizontalMultiplier;
        
        // Apply the velocity with scaled horizontal movement
        player.setDeltaMovement(scaledHorizontalX, jumpVelocity, scaledHorizontalZ);
        
        // Mark coyote time as used
        useCoyoteTime(player);
        
        if (Config.coyoteTimeDebug) {
            System.out.println("[CoyoteTime] Applied coyote time jump velocity: " + jumpVelocity + " (horizontalSpeed: " + String.format("%.3f", horizontalSpeed) + ", horizontalMultiplier: " + String.format("%.3f", horizontalMultiplier) + ", scaled horizontal velocity: " + String.format("%.3f", scaledHorizontalX) + ", " + String.format("%.3f", scaledHorizontalZ) + ")");
        }
    }
    
    /**
     * Reset coyote time state for a player (e.g., when they land on ground)
     */
    public static void resetCoyoteTime(Player player) {
        CoyoteTimeState state = getState(player);
        if (state != null) {
            state.reset();
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!Config.enableCoyoteTimeJump) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        
        UUID playerId = player.getUUID();
        CoyoteTimeState state = SERVER_PLAYER_COYOTE_STATES.computeIfAbsent(playerId, k -> new CoyoteTimeState());
        
        // Update ground state tracking
        boolean onGround = player.onGround() || player.isInWater() || player.isInLava();
        Vec3 position = player.position();
        double velocityY = player.getDeltaMovement().y;
        state.updateGroundState(onGround, position, velocityY, player);
        
        // Clean up expired states (players who haven't been active for a while)
        if (state.coyoteTimeStart > 0) {
            long currentTime = System.currentTimeMillis();
            long coyoteTimeMs = Config.coyoteTimeWindowMs;
            if ((currentTime - state.coyoteTimeStart) > coyoteTimeMs + 10000) { // 10 seconds after coyote time expires
                SERVER_PLAYER_COYOTE_STATES.remove(playerId);
            }
        }
    }
    
    /**
     * Clean up coyote time states for players who have disconnected
     */
    public static void cleanupPlayer(UUID playerId) {
        // Defensive: this can be invoked from either side depending on callsite.
        CLIENT_PLAYER_COYOTE_STATES.remove(playerId);
        SERVER_PLAYER_COYOTE_STATES.remove(playerId);
    }
    
    /**
     * Update coyote time state for client-side players
     */
    public static void updateClientState(Player player) {
        if (player.level().isClientSide()) {
            CoyoteTimeState state = getOrCreateState(player);
            
            // Update ground state tracking
            boolean onGround = player.onGround() || player.isInWater() || player.isInLava();
            Vec3 position = player.position();
            double velocityY = player.getDeltaMovement().y;
            state.updateGroundState(onGround, position, velocityY, player);
        }
    }
}
