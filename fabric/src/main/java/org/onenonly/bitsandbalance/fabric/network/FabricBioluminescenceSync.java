package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;

public final class FabricBioluminescenceSync {
    private FabricBioluminescenceSync() {
    }

    public static void initServer() {
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (!(trackedEntity instanceof LivingEntity living)) {
                return;
            }

            int remainingTicks = BioluminescenceLightManager.getBioluminescenceRemainingTicks(living);
            if (remainingTicks > 0) {
                ServerPlayNetworking.send(player, new BioluminescenceSyncPayload(living.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(living)));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                if (!(handler.getPlayer() instanceof LivingEntity living)) {
                    return;
                }

                int remainingTicks = BioluminescenceLightManager.getBioluminescenceRemainingTicks(living);
                if (remainingTicks > 0) {
                    ServerPlayNetworking.send(handler.getPlayer(), new BioluminescenceSyncPayload(living.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(living)));
                }
            });
        });
    }

    public static void initClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(BioluminescenceSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> applyClientState(payload.entityId(), payload.remainingTicks(), payload.amplifier()));
        });
    }

    public static void syncIfServer(LivingEntity entity, int remainingTicks) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        BioluminescenceSyncPayload payload = new BioluminescenceSyncPayload(entity.getId(), remainingTicks, BioluminescenceLightManager.getBioluminescenceAmplifier(entity));
        if (entity instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
        for (ServerPlayer trackingPlayer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(trackingPlayer, payload);
        }
    }

    private static void applyClientState(int entityId, int remainingTicks, int amplifier) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        BioluminescenceLightManager.syncClientBioluminescence(entityId, remainingTicks, amplifier);
    }
}