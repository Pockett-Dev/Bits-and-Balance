package org.onenonly.bitsandbalance.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class SoulFireBurnSync {
    private SoulFireBurnSync() {
    }

    public static void syncIfServer(Entity entity, boolean soulFireBurn) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        SoulFireBurnSyncPayload payload = new SoulFireBurnSyncPayload(entity.getId(), soulFireBurn);
        if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, payload);
        }
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer tracker)) {
            return;
        }
        if (!(event.getTarget() instanceof Entity trackedEntity)) {
            return;
        }
        if (!(trackedEntity instanceof SoulFireBurnAccess access)) {
            return;
        }

        PacketDistributor.sendToPlayer(tracker, new SoulFireBurnSyncPayload(trackedEntity.getId(), access.bitsandbalance$isSoulFireBurn()));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player instanceof SoulFireBurnAccess access)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, new SoulFireBurnSyncPayload(player.getId(), access.bitsandbalance$isSoulFireBurn()));
    }
}