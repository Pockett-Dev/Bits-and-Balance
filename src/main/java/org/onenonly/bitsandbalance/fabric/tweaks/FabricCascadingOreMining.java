package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining;
import org.onenonly.bitsandbalance.fabric.config.FabricEnchantingConfig;

/** Fabric hooks for the server-side Cascading sequence. */
public final class FabricCascadingOreMining {
    private FabricCascadingOreMining() {}

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
                CascadingOreMining.start(serverPlayer, pos, state,
                        FabricEnchantingConfig.enableCascading,
                        FabricEnchantingConfig.cascadingMaxActiveLevel,
                        FabricEnchantingConfig.cascadingLevelOneMaxOres,
                        FabricEnchantingConfig.cascadingLevelTwoMaxOres,
                        FabricEnchantingConfig.cascadingLevelThreeMaxOres);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(CascadingOreMining::tickAll);
    }
}
