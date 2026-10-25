package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FabricEnhancedSlabMaterialSync {
    private static final Map<UUID, TrackedView> TRACKED_VIEWS = new HashMap<>();

    private FabricEnhancedSlabMaterialSync() {
    }

    public static void initServer() {
        EnhancedSlabMaterialSavedData.setSyncCallback((level, update) -> {
            if (!(level instanceof ServerLevel serverLevel) || update == null) {
                return;
            }
            EnhancedSlabMaterialUpdatePayload payload = EnhancedSlabMaterialUpdatePayload.of(update.dimensionId(), update.pos(), update.slabs());
            sendUpdateToTrackingAndNearby(serverLevel, update.pos(), payload);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> sendBulkAround(handler.getPlayer(), true)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> TRACKED_VIEWS.remove(handler.getPlayer().getUUID())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendBulkAround(player, false);
            }
        });
    }

    private static void sendBulkAround(ServerPlayer player, boolean force) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        String dimensionId = EnhancedSlabMaterialSavedData.dimensionId(level);
        ChunkPos chunk = player.chunkPosition();
        int radius = Math.max(2, level.getServer().getPlayerList().getViewDistance());
        TrackedView view = new TrackedView(dimensionId, chunk.x, chunk.z, radius);
        TrackedView previous = TRACKED_VIEWS.get(player.getUUID());
        if (!force && view.equals(previous)) {
            return;
        }

        List<EnhancedSlabMaterialSavedData.MaterialUpdate> entries =
                EnhancedSlabMaterialSavedData.get(level).snapshotAroundChunk(dimensionId, chunk.x, chunk.z, radius);

        long[] positions = new long[entries.size()];
        net.minecraft.world.level.block.state.BlockState[][] slabs = new net.minecraft.world.level.block.state.BlockState[entries.size()][];
        for (int i = 0; i < entries.size(); i++) {
            EnhancedSlabMaterialSavedData.MaterialUpdate entry = entries.get(i);
            positions[i] = entry.pos();
            slabs[i] = entry.slabs();
        }
        ServerPlayNetworking.send(player, EnhancedSlabMaterialBulkPayload.of(dimensionId, chunk.x, chunk.z, radius, positions, slabs));
        TRACKED_VIEWS.put(player.getUUID(), view);
    }

    private static void sendUpdateToTrackingAndNearby(ServerLevel level, long posLong, EnhancedSlabMaterialUpdatePayload payload) {
        BlockPos pos = BlockPos.of(posLong);
        Set<ServerPlayer> sent = new HashSet<>();
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            if (sent.add(player)) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        double radius = (Math.max(4, level.getServer().getPlayerList().getViewDistance()) + 1) * 16.0D;
        double radiusSqr = radius * radius;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (ServerPlayer player : level.players()) {
            if (sent.contains(player) || player.distanceToSqr(x, y, z) > radiusSqr) {
                continue;
            }
            sent.add(player);
            ServerPlayNetworking.send(player, payload);
        }
    }

    private record TrackedView(String dimensionId, int chunkX, int chunkZ, int radius) {
    }
}
