package org.onenonly.bitsandbalance.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class BioluminescenceSync {
    private BioluminescenceSync() {
    }

    public static void syncIfServer(LivingEntity entity, int remainingTicks) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        BioluminescenceSyncPayload payload = new BioluminescenceSyncPayload(entity.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(entity));
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
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }

        int remainingTicks = BioluminescenceLightManager.getBioluminescenceRemainingTicks(living);
        if (remainingTicks > 0) {
            PacketDistributor.sendToPlayer(tracker, new BioluminescenceSyncPayload(living.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(living)));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int remainingTicks = BioluminescenceLightManager.getBioluminescenceRemainingTicks(player);
        if (remainingTicks > 0) {
            PacketDistributor.sendToPlayer(player, new BioluminescenceSyncPayload(player.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(player)));
        }
    }
}