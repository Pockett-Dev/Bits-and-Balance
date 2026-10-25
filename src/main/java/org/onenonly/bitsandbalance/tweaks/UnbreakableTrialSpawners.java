package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//? if mc == 26.2 || mc == 26.3
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
//? else
import net.neoforged.neoforge.event.level.BlockEvent;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

/**
 * Tweaks: Unbreakable Trial Blocks
 * When enabled, Trial Spawners and Vaults cannot be broken by any means.
 * Server-side only.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class UnbreakableTrialSpawners {

    @SubscribeEvent
    public static void onBlockBreak(
//? if mc == 26.2 || mc == 26.3
            BreakBlockEvent event
//? else
            BlockEvent.BreakEvent event
//? endif
    ) {
        if (event.getPlayer() != null && event.getPlayer().isCreative()) return;

        BlockState state = event.getState();
        if (isProtected(state)) {
            event.setCanceled(true);
        }
    }

    private static boolean isProtected(BlockState state) {
        return (Config.enableUnbreakableTrialSpawners && state.is(Blocks.TRIAL_SPAWNER))
                || (Config.enableUnbreakableVaults && state.is(Blocks.VAULT));
    }
}
