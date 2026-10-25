package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric port: Rapid Fire Jump
 * Hold jump key for continuous jumping within ceiling / 2-block gap scenarios.
 */
public final class FabricRapidFireJump {
    private FabricRapidFireJump() {
    }

    private static final Map<UUID, RapidFireJumpState> STATES = new HashMap<>();

    private static final class RapidFireJumpState {
        private int ticksSinceLastJump;
        private boolean wasJumpKeyPressed;
        private boolean isOnGround;

        void update(boolean jumpKeyPressed, boolean onGround) {
            this.wasJumpKeyPressed = jumpKeyPressed;
            this.isOnGround = onGround;

            if (jumpKeyPressed) {
                this.ticksSinceLastJump++;
            } else {
                this.ticksSinceLastJump = 0;
            }
        }

        boolean canRapidFireJump() {
            if (!this.wasJumpKeyPressed) return false;
            if (!this.isOnGround) return false;
            return this.ticksSinceLastJump >= FabricMechanicsConfig.rapidFireJumpInterval;
        }

        void executedJump() {
            this.ticksSinceLastJump = 0;
        }
    }

    public static void initClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricMechanicsConfig.enableRapidFireJump) return;

            LocalPlayer player = client.player;
            if (player == null) return;

            boolean jumpKeyPressed = client.options.keyJump.isDown();

            RapidFireJumpState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new RapidFireJumpState());
            state.update(jumpKeyPressed, player.onGround());

            if (!jumpKeyPressed) return;
            if (!isInCeilingOrGapScenario(player)) return;

            if (state.canRapidFireJump()) {
                player.jumpFromGround();
                state.executedJump();
            }
        });
    }

    private static boolean isInCeilingOrGapScenario(LocalPlayer player) {
        Level level = player.level();
        if (level == null) return false;

        AABB playerBB = player.getBoundingBox();
        return isHittingCeiling(player, level, playerBB) || isWithinTwoBlockGap(player, level, playerBB);
    }

    private static boolean isHittingCeiling(LocalPlayer player, Level level, AABB playerBB) {
        BlockPos headPos = BlockPos.containing(player.getX(), playerBB.maxY, player.getZ());
        BlockState headBlock = level.getBlockState(headPos);
        return !headBlock.isAir() && !headBlock.getCollisionShape(level, headPos).isEmpty();
    }

    private static boolean isWithinTwoBlockGap(LocalPlayer player, Level level, AABB playerBB) {
        double playerTop = playerBB.maxY;

        for (int y = 1; y <= 2; y++) {
            BlockPos ceilingPos = BlockPos.containing(player.getX(), playerTop + y, player.getZ());
            BlockState ceilingBlock = level.getBlockState(ceilingPos);
            if (!ceilingBlock.isAir() && !ceilingBlock.getCollisionShape(level, ceilingPos).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
