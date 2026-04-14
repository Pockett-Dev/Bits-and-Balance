package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class DoubleDoorOpeningFallbackEvents {
    private DoubleDoorOpeningFallbackEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableDoubleDoorOpening || !Config.doubleDoorChainTrapdoors) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!TrapDoorInteractionFallbackHelper.shouldUseInteractionFallback(state)) return;

        if (TrapDoorInteractionFallbackHelper.handlePlayerTrapdoorInteraction(event.getLevel(), event.getPos(), player)) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }
}