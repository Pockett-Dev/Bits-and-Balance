package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabLegacyTagMigration;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.mixin.ChunkAccessPendingBlockEntitiesAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FabricEnhancedSlabLegacyMigration {
    private static final int MAX_MIGRATIONS_PER_TICK = 512;
    private static final ArrayDeque<PendingMigration> PENDING = new ArrayDeque<>();
    private static final Set<PendingMigration> PENDING_KEYS = new HashSet<>();
    private static final Map<PendingMigration, BlockState[]> PENDING_MATERIALS = new HashMap<>();
    private static final Object QUEUE_LOCK = new Object();
    private static boolean initialized;
    private static int queuedLiveBlockEntities;
    private static int queuedPendingTags;
    private static int rejectedBlockEntities;
    private static int rejectedPendingTags;
    private static int migratedLiveBlockEntities;
    private static int migratedQueuedMaterials;
    private static int optimizedSavedSlotPositions;

    private FabricEnhancedSlabLegacyMigration() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerChunkEvents.CHUNK_LOAD.register(FabricEnhancedSlabLegacyMigration::queueChunk);
        ServerTickEvents.END_SERVER_TICK.register(FabricEnhancedSlabLegacyMigration::drainQueue);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EnhancedSlabMaterialSavedData.clearPortableBlockEntities(server);
            synchronized (QUEUE_LOCK) {
                PENDING.clear();
                PENDING_KEYS.clear();
                PENDING_MATERIALS.clear();
                resetDiagnostics();
            }
        });
    }

    public static String diagnosticSummary() {
        synchronized (QUEUE_LOCK) {
            return "pending=" + PENDING.size()
                    + ",pendingMaterials=" + PENDING_MATERIALS.size()
                    + ",queuedLiveBEs=" + queuedLiveBlockEntities
                    + ",queuedPendingTags=" + queuedPendingTags
                    + ",rejectedBEs=" + rejectedBlockEntities
                    + ",rejectedPendingTags=" + rejectedPendingTags
                    + ",migratedLiveBEs=" + migratedLiveBlockEntities
                    + ",migratedQueuedMaterials=" + migratedQueuedMaterials
                    + ",optimizedSavedPositions=" + optimizedSavedSlotPositions;
        }
    }

    private static void resetDiagnostics() {
        queuedLiveBlockEntities = 0;
        queuedPendingTags = 0;
        rejectedBlockEntities = 0;
        rejectedPendingTags = 0;
        migratedLiveBlockEntities = 0;
        migratedQueuedMaterials = 0;
        optimizedSavedSlotPositions = 0;
    }

    public static boolean migrateRejectedBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        return migrateRejectedBlockEntity(level, blockEntity, blockEntity == null ? null : blockEntity.getBlockState());
    }

    public static boolean migrateRejectedBlockEntity(ServerLevel level, BlockEntity blockEntity, BlockState state) {
        if (level == null || blockEntity == null || state == null || !isEnhancedSlabBlockEntity(blockEntity)) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (state.getBlock() instanceof EntityBlock) {
            return false;
        }

        boolean enqueued = enqueueMaterials(level.dimension(), pos.asLong(), savedOrBlockEntityMaterials(level, blockEntity));
        if (enqueued) {
            incrementRejectedBlockEntities();
        }
        return enqueued;
    }

    public static boolean migrateRejectedBlockEntityTag(ServerLevel level, BlockPos pos, CompoundTag tag) {
        return migrateRejectedBlockEntityTag(level, pos, tag, null);
    }

    public static boolean migrateRejectedBlockEntityTag(ServerLevel level, BlockPos pos, CompoundTag tag, BlockState state) {
        if (level == null || pos == null || state == null || !EnhancedSlabLegacyTagMigration.isEnhancedSlabBlockEntityTag(tag)) {
            return false;
        }

        if (!EnhancedSlabLegacyTagMigration.isEnhancedSlabState(state) || state.hasBlockEntity()) {
            return false;
        }

        boolean enqueued = enqueueMaterials(level.dimension(), pos.asLong(), EnhancedSlabLegacyTagMigration.readSlots(tag));
        if (enqueued) {
            incrementRejectedPendingTags();
        }
        return enqueued;
    }

    private static void queueChunk(ServerLevel level, LevelChunk chunk) {
        EnhancedSlabMaterialSavedData.queuePortableBlockEntities(level, chunk);
        if (level == null || chunk == null) {
            return;
        }

        if (!chunk.getBlockEntities().isEmpty()) {
            int queued = 0;
            for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                if (isEnhancedSlabBlockEntity(blockEntity)) {
                    enqueue(level.dimension(), blockEntity.getBlockPos().asLong());
                    queued++;
                }
            }
            addQueuedLiveBlockEntities(queued);
        }

        addQueuedPendingTags(queuePendingBlockEntityTags(level, chunk));
    }

    private static int queuePendingBlockEntityTags(ServerLevel level, LevelChunk chunk) {
        Map<BlockPos, CompoundTag> pendingTags = pendingBlockEntityTags(chunk);
        if (pendingTags == null || pendingTags.isEmpty()) {
            return 0;
        }

        int queued = 0;
        for (Map.Entry<BlockPos, CompoundTag> entry : pendingTags.entrySet()) {
            BlockPos pos = entry.getKey();
            CompoundTag tag = entry.getValue();
            if (pos != null && EnhancedSlabLegacyTagMigration.isEnhancedSlabBlockEntityTag(tag)) {
                enqueue(level.dimension(), pos.asLong());
                queued++;
            }
        }
        return queued;
    }

    private static Map<BlockPos, CompoundTag> pendingBlockEntityTags(LevelChunk chunk) {
        try {
            return ((ChunkAccessPendingBlockEntitiesAccessor) (Object) chunk).bitsandbalance$getPendingBlockEntities();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void enqueue(ResourceKey<Level> dimension, long posLong) {
        synchronized (QUEUE_LOCK) {
            PendingMigration pending = new PendingMigration(dimension, posLong);
            if (PENDING_KEYS.add(pending)) {
                PENDING.addLast(pending);
            }
        }
    }

    private static boolean enqueueMaterials(ResourceKey<Level> dimension, long posLong, BlockState[] materials) {
        if (materials == null || isEmpty(materials)) {
            return false;
        }

        synchronized (QUEUE_LOCK) {
            PendingMigration pending = new PendingMigration(dimension, posLong);
            PENDING_MATERIALS.put(pending, materials.clone());
            if (PENDING_KEYS.add(pending)) {
                PENDING.addLast(pending);
            }
        }
        return true;
    }

    private static void drainQueue(MinecraftServer server) {
        EnhancedSlabMaterialSavedData.drainPortableBlockEntities(server);
        if (server == null || isQueueEmpty()) {
            return;
        }

        int processed = 0;
        while (processed++ < MAX_MIGRATIONS_PER_TICK) {
            PendingMigration pending = pollPending();
            if (pending == null) {
                return;
            }

            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) {
                continue;
            }

            BlockPos pos = BlockPos.of(pending.posLong());
            if (!level.isLoaded(pos)) {
                if (hasQueuedMaterials(pending)) {
                    enqueue(pending.dimension(), pending.posLong());
                }
                continue;
            }

            BlockState[] queuedMaterials = removeQueuedMaterials(pending);
            if (queuedMaterials != null) {
                BlockState[] savedMaterials = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                migrateSavedSlotsPosition(level, pos,
                        savedMaterials == null || isEmpty(savedMaterials) ? queuedMaterials : savedMaterials);
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                migrateLiveBlockEntity(level, blockEntity);
            } else {
                BlockState[] promotedMaterials = removeQueuedMaterials(pending);
                if (promotedMaterials != null) {
                    migrateSavedSlotsPosition(level, pos, promotedMaterials);
                } else {
                    migrateSavedSlotsPosition(level, pos);
                }
            }
        }
    }

    private static void migrateLiveBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        BlockState beforeState = level.getBlockState(pos);
        BlockState[] materials = savedOrBlockEntityMaterials(level, blockEntity);
        if (materials == null || isEmpty(materials)) {
            return;
        }

        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), materials);
        EnhancedSlabStorageOptimizer.optimizeAt(level, pos);
        BlockState afterState = level.getBlockState(pos);
        if (!(afterState.getBlock() instanceof EntityBlock)) {
            level.removeBlockEntity(pos);
        }
        level.sendBlockUpdated(pos, beforeState, afterState, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);
        incrementMigratedLiveBlockEntities();
    }

    private static void migrateSavedSlotsPosition(ServerLevel level, BlockPos pos, BlockState[] materials) {
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), materials);
        incrementMigratedQueuedMaterials();
        migrateSavedSlotsPosition(level, pos);
    }

    private static void migrateSavedSlotsPosition(ServerLevel level, BlockPos pos) {
        if (EnhancedSlabMaterialSavedData.get(level, pos.asLong()) == null) {
            return;
        }

        BlockState beforeState = level.getBlockState(pos);
        EnhancedSlabStorageOptimizer.optimizeAt(level, pos);
        BlockState afterState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, beforeState, afterState, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);
        incrementOptimizedSavedSlotPositions();
    }

    private static @Nullable BlockState[] savedOrBlockEntityMaterials(ServerLevel level, BlockEntity blockEntity) {
        BlockState[] saved = EnhancedSlabMaterialSavedData.get(level, blockEntity.getBlockPos().asLong());
        if (saved != null && !isEmpty(saved)) {
            return saved;
        }
        return readBlockEntityMaterials(blockEntity);
    }

    private static boolean isQueueEmpty() {
        synchronized (QUEUE_LOCK) {
            return PENDING.isEmpty();
        }
    }

    private static PendingMigration pollPending() {
        synchronized (QUEUE_LOCK) {
            PendingMigration pending = PENDING.pollFirst();
            if (pending != null) {
                PENDING_KEYS.remove(pending);
            }
            return pending;
        }
    }

    private static boolean hasQueuedMaterials(PendingMigration pending) {
        synchronized (QUEUE_LOCK) {
            return PENDING_MATERIALS.containsKey(pending);
        }
    }

    private static BlockState[] removeQueuedMaterials(PendingMigration pending) {
        synchronized (QUEUE_LOCK) {
            return PENDING_MATERIALS.remove(pending);
        }
    }

    private static void addQueuedLiveBlockEntities(int count) {
        if (count <= 0) {
            return;
        }
        synchronized (QUEUE_LOCK) {
            queuedLiveBlockEntities += count;
        }
    }

    private static void addQueuedPendingTags(int count) {
        if (count <= 0) {
            return;
        }
        synchronized (QUEUE_LOCK) {
            queuedPendingTags += count;
        }
    }

    private static void incrementRejectedBlockEntities() {
        synchronized (QUEUE_LOCK) {
            rejectedBlockEntities++;
        }
    }

    private static void incrementRejectedPendingTags() {
        synchronized (QUEUE_LOCK) {
            rejectedPendingTags++;
        }
    }

    private static void incrementMigratedLiveBlockEntities() {
        synchronized (QUEUE_LOCK) {
            migratedLiveBlockEntities++;
        }
    }

    private static void incrementMigratedQueuedMaterials() {
        synchronized (QUEUE_LOCK) {
            migratedQueuedMaterials++;
        }
    }

    private static void incrementOptimizedSavedSlotPositions() {
        synchronized (QUEUE_LOCK) {
            optimizedSavedSlotPositions++;
        }
    }

    private static BlockState[] readBlockEntityMaterials(BlockEntity blockEntity) {
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new BlockState[]{verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab()};
        } else if (blockEntity instanceof StepBlockEntity step) {
            return new BlockState[]{step.getSlabState()};
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            return new BlockState[]{verticalStep.getSlabState()};
        } else if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            return quadStep.getAllSlabs();
        } else if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return quadVerticalStep.getAllSlabs();
        }

        return null;
    }

    private static boolean isEmpty(BlockState[] materials) {
        if (materials == null) {
            return true;
        }
        for (BlockState material : materials) {
            if (material != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEnhancedSlabBlockEntity(BlockEntity blockEntity) {
        return blockEntity instanceof VerticalSlabBlockEntity
                || blockEntity instanceof StepBlockEntity
                || blockEntity instanceof VerticalStepBlockEntity
                || blockEntity instanceof QuadStepBlockEntity
                || blockEntity instanceof QuadVerticalStepBlockEntity;
    }

    private record PendingMigration(ResourceKey<Level> dimension, long posLong) {
    }
}
