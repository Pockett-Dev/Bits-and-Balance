package org.onenonly.bitsandbalance.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.compat.SableCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class EnhancedSlabMaterialSync {
    private static final int MATERIAL_UPDATE_RETRY_TICKS = 20;
    private static final Map<UUID, TrackedView> TRACKED_VIEWS = new HashMap<>();
    private static final Map<PendingUpdateKey, PendingUpdate> PENDING_UPDATES = new HashMap<>();
    private static boolean initialized;

    private EnhancedSlabMaterialSync() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        EnhancedSlabMaterialSavedData.setSyncCallback((level, update) -> {
            if (!(level instanceof ServerLevel serverLevel) || update == null) {
                return;
            }

            EnhancedSlabMaterialUpdatePayload payload =
                    EnhancedSlabMaterialUpdatePayload.of(update.dimensionId(), update.pos(), update.slabs());
            if (!sendUpdateToNearby(serverLevel, update.pos(), payload)) {
                PendingUpdateKey key = new PendingUpdateKey(update.dimensionId(), update.pos());
                PENDING_UPDATES.put(key, new PendingUpdate(serverLevel, payload, MATERIAL_UPDATE_RETRY_TICKS));
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendBulkAround(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendBulkAround(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TRACKED_VIEWS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        retryPendingUpdates();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            sendBulkAround(player, false);
        }
    }

    private static void sendBulkAround(ServerPlayer player, boolean force) {
        if (!(player.level() instanceof ServerLevel level)) {
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

        sendIndexedBulk(player, level, dimensionId, chunk.x, chunk.z, radius);
        TRACKED_VIEWS.put(player.getUUID(), view);
    }

    /**
     * Called after Sable sends its ordered StartTracking + chunks + Finalize bundle. Each plot
     * chunk is synchronized from the material chunk index, avoiding the former whole-map scan on
     * every ordinary player view change while keeping remote sublevel materials ordered correctly.
     */
    public static void onSableFullSync(ServerPlayer player, Object serverSubLevel) {
        if (player == null || serverSubLevel == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        try {
            Object plot = serverSubLevel.getClass().getMethod("getPlot").invoke(serverSubLevel);
            if (plot == null) {
                return;
            }
            Object loadedChunks = plot.getClass().getMethod("getLoadedChunks").invoke(plot);
            if (!(loadedChunks instanceof Iterable<?> holders)) {
                return;
            }

            String dimensionId = EnhancedSlabMaterialSavedData.dimensionId(level);
            LongOpenHashSet sentChunks = new LongOpenHashSet();
            for (Object holder : holders) {
                if (holder == null) {
                    continue;
                }
                Object chunk = holder.getClass().getMethod("getChunk").invoke(holder);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }
                ChunkPos chunkPos = levelChunk.getPos();
                if (sentChunks.add(chunkPos.toLong())) {
                    sendIndexedBulk(player, level, dimensionId, chunkPos.x, chunkPos.z, 0);
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Sable is optional and has changed internals between releases. Its own full sync must
            // remain usable if a future version no longer exposes the plot chunk collection.
        }
    }

    private static void sendIndexedBulk(ServerPlayer player,
                                        ServerLevel level,
                                        String dimensionId,
                                        int centerChunkX,
                                        int centerChunkZ,
                                        int radius) {
        List<EnhancedSlabMaterialSavedData.MaterialUpdate> entries = EnhancedSlabMaterialSavedData.get(level)
                .snapshotAroundChunk(dimensionId, centerChunkX, centerChunkZ, radius);
        long[] positions = new long[entries.size()];
        BlockState[][] slabs = new BlockState[entries.size()][];
        for (int i = 0; i < entries.size(); i++) {
            EnhancedSlabMaterialSavedData.MaterialUpdate entry = entries.get(i);
            positions[i] = entry.pos();
            slabs[i] = entry.slabs();
        }
        PacketDistributor.sendToPlayer(player, EnhancedSlabMaterialBulkPayload.of(
                dimensionId, centerChunkX, centerChunkZ, radius, positions, slabs));
    }

    private static void retryPendingUpdates() {
        var iterator = PENDING_UPDATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PendingUpdateKey, PendingUpdate> entry = iterator.next();
            PendingUpdate pending = entry.getValue();
            if (pending == null || pending.remainingTicks() <= 0) {
                iterator.remove();
                continue;
            }
            if (sendUpdateToNearby(pending.level(), entry.getKey().pos(), pending.payload())) {
                iterator.remove();
                continue;
            }
            entry.setValue(new PendingUpdate(
                    pending.level(),
                    pending.payload(),
                    pending.remainingTicks() - 1
            ));
        }
    }

    private static boolean sendUpdateToNearby(ServerLevel level,
                                              long posLong,
                                              EnhancedSlabMaterialUpdatePayload payload) {
        BlockPos pos = BlockPos.of(posLong);
        double radius = (Math.max(4, level.getServer().getPlayerList().getViewDistance()) + 1) * 16.0D;
        double radiusSqr = radius * radius;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        boolean plotGridPosition = SableCompat.isInPlotGrid(level, pos.getX() >> 4, pos.getZ() >> 4);
        boolean sent = false;
        boolean sentToTrackingPlayer = false;
        for (ServerPlayer player : level.players()) {
            boolean trackingPlot = SableCompat.isPlayerTrackingPlotPosition(level, pos, player.getUUID());
            if (player.distanceToSqr(x, y, z) <= radiusSqr
                    || plotGridPosition
                    || trackingPlot) {
                PacketDistributor.sendToPlayer(player, payload);
                sent = true;
                sentToTrackingPlayer |= trackingPlot;
            }
        }
        // Sable allocates the remote plot before it sends StartTracking + chunk + Finalize to the
        // client. A packet broadcast merely because the grid already exists is therefore too early
        // to build render data. Keep retrying plot updates until at least one actual tracking player
        // receives the packet after that ordered full sync. Ordinary world updates still complete
        // as soon as any nearby player receives them.
        return sent && (!plotGridPosition || sentToTrackingPlayer);
    }

    private record TrackedView(String dimensionId, int chunkX, int chunkZ, int radius) {
    }

    private record PendingUpdateKey(String dimensionId, long pos) {
    }

    private record PendingUpdate(ServerLevel level,
                                 EnhancedSlabMaterialUpdatePayload payload,
                                 int remainingTicks) {
    }
}
