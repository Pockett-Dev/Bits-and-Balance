package org.onenonly.bitsandbalance.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentAccess;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class EnhancedSlabHangIntentSync {
    private EnhancedSlabHangIntentSync() {
    }

    public static void sendUpdateToAll(ServerLevel level, long posLong, boolean explicit) {
        EnhancedSlabHangIntentUpdatePayload payload = new EnhancedSlabHangIntentUpdatePayload(posLong, explicit);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendBulkTo(ServerPlayer player, ServerLevel level) {
        PacketDistributor.sendToPlayer(player, new EnhancedSlabHangIntentBulkPayload(EnhancedSlabHangIntentAccess.get(level).snapshot()));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        sendBulkTo(player, level);
    }
}