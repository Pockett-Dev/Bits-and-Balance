package org.onenonly.bitsandbalance.tweaks;

import org.onenonly.bitsandbalance.common.config.DoubleDoorFeatureFlags;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class DoubleDoorOpeningFallbackEvents {
    private DoubleDoorOpeningFallbackEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled() || !DoubleDoorFeatureFlags.doubleDoorChainTrapdoorsEnabled()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!TrapDoorInteractionFallbackHelper.shouldUseInteractionFallback(state)) return;

        TrapDoorInteractionFallbackHelper.schedulePostInteractionTrapdoorChain(event.getLevel(), event.getPos(), player, state);
    }
}
