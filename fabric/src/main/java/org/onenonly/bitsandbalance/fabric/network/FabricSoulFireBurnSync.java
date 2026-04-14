package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;

public final class FabricSoulFireBurnSync {
    private FabricSoulFireBurnSync() {
    }

    public static void initServer() {
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (!(trackedEntity instanceof SoulFireBurnAccess access)) {
                return;
            }

            ServerPlayNetworking.send(player, new SoulFireBurnSyncPayload(trackedEntity.getId(), access.bitsandbalance$isSoulFireBurn()));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                if (!(handler.getPlayer() instanceof SoulFireBurnAccess access)) {
                    return;
                }
                ServerPlayNetworking.send(handler.getPlayer(), new SoulFireBurnSyncPayload(handler.getPlayer().getId(), access.bitsandbalance$isSoulFireBurn()));
            });
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // No persistent sync cache needed; hook exists to keep lifecycle parity with other networking helpers.
        });
    }

    public static void initClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(SoulFireBurnSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> applyClientState(payload.entityId(), payload.soulFireBurn()));
        });
    }

    public static void syncIfServer(Entity entity, boolean soulFireBurn) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        SoulFireBurnSyncPayload payload = new SoulFireBurnSyncPayload(entity.getId(), soulFireBurn);
        if (entity instanceof Player player && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
        for (net.minecraft.server.level.ServerPlayer trackingPlayer : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(trackingPlayer, payload);
        }
    }

    private static void applyClientState(int entityId, boolean soulFireBurn) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        var entity = level.getEntity(entityId);
        if (entity instanceof SoulFireBurnAccess access) {
            access.bitsandbalance$setSoulFireBurn(soulFireBurn);
        }
    }
}