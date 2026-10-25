package org.onenonly.bitsandbalance.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.common.mechanics.BiomeTitlesStructureTracker;

public final class BiomeTitlesStructureSync {
    private BiomeTitlesStructureSync() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        BiomeTitlesStructureTracker.tick(player, BiomeTitlesStructureSync::send);
    }

    private static void send(ServerPlayer player, boolean insideStructure) {
        PacketDistributor.sendToPlayer(player, new BiomeTitlesStructurePayload(insideStructure));
    }
}
