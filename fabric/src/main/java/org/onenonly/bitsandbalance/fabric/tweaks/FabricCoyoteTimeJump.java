package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric port: Coyote Time Jump
 * Allows players to jump for a short time after falling off a block.
 */
public final class FabricCoyoteTimeJump {
    private FabricCoyoteTimeJump() {
    }

    private static final Map<UUID, CoyoteTimeState> PLAYER_STATES = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMechanicsConfig.enableCoyoteTimeJump) return;

            for (var player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(Player player) {
        CoyoteTimeState state = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new CoyoteTimeState());
        
        Vec3 position = player.position();
        double velocityY = player.getDeltaMovement().y;
        boolean onGround = player.onGround();

        state.updateGroundState(onGround, position, velocityY, player);
    }

    public static boolean canUseCoyoteTime(Player player) {
        if (!FabricMechanicsConfig.enableCoyoteTimeJump) return false;
        if (player.isInWater() || player.isInLava()) return false;

        CoyoteTimeState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) return false;

        return state.canJump();
    }

    public static void markCoyoteTimeUsed(Player player) {
        CoyoteTimeState state = PLAYER_STATES.get(player.getUUID());
        if (state != null) {
            state.markUsed();
        }
    }

    private static class CoyoteTimeState {
        private long coyoteTimeStart;
        private boolean wasOnGround;
        private boolean hasUsedCoyoteTime;
        private boolean hasJumped;
        private Vec3 lastPosition;
        private double lastVelocityY;
        private long lastGroundTime;
        private double fallStartY;

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
                this.coyoteTimeStart = 0;
                this.wasOnGround = false;
                this.hasUsedCoyoteTime = false;
                this.hasJumped = false;
                this.lastGroundTime = 0;
                this.fallStartY = 0.0;
                return;
            }

            if (this.wasOnGround && !onGround && velocityY > 0) {
                this.hasJumped = true;
            }

            if (onGround) {
                this.wasOnGround = true;
                this.hasJumped = false;
                this.coyoteTimeStart = 0;
                this.lastPosition = position;
                this.lastVelocityY = velocityY;

                long currentTime = System.currentTimeMillis();
                if (this.lastGroundTime == 0) {
                    this.lastGroundTime = currentTime;
                } else if ((currentTime - this.lastGroundTime) >= 200) {
                    this.hasUsedCoyoteTime = false;
                }
            } else {
                this.lastGroundTime = 0;

                if (this.wasOnGround && !this.hasUsedCoyoteTime && this.coyoteTimeStart == 0 && !this.hasJumped) {
                    if (velocityY <= 0 && !player.isFallFlying() && !player.isPassenger()) {
                        this.coyoteTimeStart = System.currentTimeMillis();
                        this.fallStartY = position.y;
                        this.wasOnGround = false;
                    }
                }
            }

            this.lastPosition = position;
            this.lastVelocityY = velocityY;
        }

        public boolean canJump() {
            if (this.coyoteTimeStart == 0 || this.hasUsedCoyoteTime) return false;

            long currentTime = System.currentTimeMillis();
            long elapsedMs = currentTime - this.coyoteTimeStart;

            int delayMs = FabricMechanicsConfig.coyoteTimeDelayMs;
            int windowMs = FabricMechanicsConfig.coyoteTimeWindowMs;
            return elapsedMs >= delayMs && elapsedMs <= windowMs;
        }

        public void markUsed() {
            this.hasUsedCoyoteTime = true;
            this.coyoteTimeStart = 0;
        }
    }
}
