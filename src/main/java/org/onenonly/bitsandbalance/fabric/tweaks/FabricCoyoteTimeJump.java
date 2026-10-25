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

    private static final Map<UUID, CoyoteTimeState> CLIENT_PLAYER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, CoyoteTimeState> SERVER_PLAYER_STATES = new ConcurrentHashMap<>();

    private static Map<UUID, CoyoteTimeState> getStateMap(Player player) {
        return player.level().isClientSide() ? CLIENT_PLAYER_STATES : SERVER_PLAYER_STATES;
    }

    private static CoyoteTimeState getOrCreateState(Player player) {
        return getStateMap(player).computeIfAbsent(player.getUUID(), k -> new CoyoteTimeState());
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!FabricMechanicsConfig.enableCoyoteTimeJump) return;

            for (var player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(Player player) {
        CoyoteTimeState state = getOrCreateState(player);
        
        Vec3 position = player.position();
        double velocityY = player.getDeltaMovement().y;
        boolean onGround = player.onGround();

        state.updateGroundState(onGround, position, velocityY, player);
    }

    public static boolean canUseCoyoteTime(Player player) {
        if (!FabricMechanicsConfig.enableCoyoteTimeJump) return false;
        if (player.isInWater() || player.isInLava()) return false;

        CoyoteTimeState state = getStateMap(player).get(player.getUUID());
        if (state == null) return false;

        return state.canJump();
    }

    public static void markCoyoteTimeUsed(Player player) {
        CoyoteTimeState state = getStateMap(player).get(player.getUUID());
        if (state != null) {
            state.markUsed();
        }
    }

    public static void updateClientState(Player player) {
        if (!FabricMechanicsConfig.enableCoyoteTimeJump || !player.level().isClientSide()) {
            return;
        }

        tickPlayer(player);
    }

    public static void performCoyoteTimeJump(Player player) {
        if (!canUseCoyoteTime(player)) {
            return;
        }

        Vec3 currentVelocity = player.getDeltaMovement();
        double jumpVelocity = 0.5D;

        //? if mc == 1.21.1
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP)) {
            jumpVelocity += 0.1D * (player.getEffect(net.minecraft.world.effect.MobEffects.JUMP).getAmplifier() + 1);
        }
        //? else
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST)) {
            jumpVelocity += 0.1D * (player.getEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST).getAmplifier() + 1);
        }
        //? endif

        double horizontalSpeed = Math.sqrt(currentVelocity.x * currentVelocity.x + currentVelocity.z * currentVelocity.z);
        double horizontalMultiplier = 1.0D + (horizontalSpeed * 3.2D);
        horizontalMultiplier = Math.min(horizontalMultiplier, 3.2D);
        horizontalMultiplier = Math.max(horizontalMultiplier, 1.5D);

        double scaledHorizontalX = currentVelocity.x * horizontalMultiplier;
        double scaledHorizontalZ = currentVelocity.z * horizontalMultiplier;
        player.setDeltaMovement(scaledHorizontalX, jumpVelocity, scaledHorizontalZ);
        player.resetFallDistance();
        markCoyoteTimeUsed(player);

        if (FabricMechanicsConfig.coyoteTimeDebug) {
            System.out.println("[CoyoteTime] Applied Fabric coyote time jump velocity: " + jumpVelocity + " (horizontalSpeed: " + String.format("%.3f", horizontalSpeed) + ", horizontalMultiplier: " + String.format("%.3f", horizontalMultiplier) + ", scaled horizontal velocity: " + String.format("%.3f", scaledHorizontalX) + ", " + String.format("%.3f", scaledHorizontalZ) + ")");
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
