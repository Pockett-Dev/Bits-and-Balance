package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//? if mc == 26.2 || mc == 26.3
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
//? else
import net.neoforged.neoforge.event.level.BlockEvent;
//? endif
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining;

/** NeoForge hooks for the server-side Cascading sequence. */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class CascadingOreMiningEvents {
    private CascadingOreMiningEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(
//? if mc == 26.2 || mc == 26.3
            BreakBlockEvent event
//? else
            BlockEvent.BreakEvent event
//? endif
    ) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getPlayer() instanceof ServerPlayer player)) return;
        CascadingOreMining.start(player, event.getPos(), event.getState(),
                Config.enableCascading, Config.cascadingMaxActiveLevel,
                Config.cascadingLevelOneMaxOres, Config.cascadingLevelTwoMaxOres,
                Config.cascadingLevelThreeMaxOres);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) CascadingOreMining.tick(player);
    }
}
