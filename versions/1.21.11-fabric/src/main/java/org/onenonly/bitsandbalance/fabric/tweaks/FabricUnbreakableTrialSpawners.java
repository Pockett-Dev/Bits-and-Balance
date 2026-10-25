package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.level.block.Blocks;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

/**
 * Fabric port: Unbreakable Trial Blocks
 * When enabled, Trial Spawners and Vaults cannot be broken by any means.
 */
public final class FabricUnbreakableTrialSpawners {
    private FabricUnbreakableTrialSpawners() {
    }

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player != null && player.isCreative()) return true;

            return !isProtected(state);
        });
    }

    private static boolean isProtected(net.minecraft.world.level.block.state.BlockState state) {
        return (FabricTweaksConfig.enableUnbreakableTrialSpawners && state.is(Blocks.TRIAL_SPAWNER))
                || (FabricTweaksConfig.enableUnbreakableVaults && state.is(Blocks.VAULT));
    }
}
