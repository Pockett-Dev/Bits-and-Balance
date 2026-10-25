package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.TrapDoorInteractionFallbackCore;

public final class TrapDoorInteractionFallbackHelper {
    private TrapDoorInteractionFallbackHelper() {
    }

    public static boolean shouldUseInteractionFallback(BlockState state) {
        return TrapDoorInteractionFallbackCore.shouldUseInteractionFallback(state);
    }

    public static void schedulePostInteractionTrapdoorChain(Level level, BlockPos pos, Player player, BlockState stateBefore) {
        TrapDoorInteractionFallbackCore.schedulePostInteractionTrapdoorChain(level, pos, player, stateBefore);
    }
}
