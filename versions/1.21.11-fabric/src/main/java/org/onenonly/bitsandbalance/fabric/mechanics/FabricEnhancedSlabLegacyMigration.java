package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
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
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class FabricEnhancedSlabLegacyMigration {
    private static final int MAX_MIGRATIONS_PER_TICK = 512;
    private static final ArrayDeque<PendingMigration> PENDING = new ArrayDeque<>();
    private static final Set<PendingMigration> PENDING_KEYS = new HashSet<>();
    private static boolean initialized;

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
            PENDING.clear();
            PENDING_KEYS.clear();
        });
    }

    public static boolean migrateRejectedBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        if (level == null || blockEntity == null || !isEnhancedSlabBlockEntity(blockEntity)) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        // The chunk may still be loading when this rejection hook runs.
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof EntityBlock) {
            return false;
        }

        return rememberBlockEntityMaterials(level, blockEntity);
    }

    private static void queueChunk(ServerLevel level, LevelChunk chunk) {
        EnhancedSlabMaterialSavedData.queuePortableBlockEntities(level, chunk);
        if (level == null || chunk == null || chunk.getBlockEntities().isEmpty()) {
            return;
        }

        for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
            if (isEnhancedSlabBlockEntity(blockEntity)) {
                enqueue(level.dimension(), blockEntity.getBlockPos().asLong());
            }
        }
    }

    private static void enqueue(ResourceKey<Level> dimension, long posLong) {
        PendingMigration pending = new PendingMigration(dimension, posLong);
        if (PENDING_KEYS.add(pending)) {
            PENDING.addLast(pending);
        }
    }

    private static void drainQueue(MinecraftServer server) {
        EnhancedSlabMaterialSavedData.drainPortableBlockEntities(server);
        if (server == null || PENDING.isEmpty()) {
            return;
        }

        int processed = 0;
        while (processed++ < MAX_MIGRATIONS_PER_TICK && !PENDING.isEmpty()) {
            PendingMigration pending = PENDING.pollFirst();
            PENDING_KEYS.remove(pending);
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) {
                continue;
            }

            BlockPos pos = BlockPos.of(pending.posLong());
            if (!level.isLoaded(pos)) {
                continue;
            }

            migrateLiveBlockEntity(level, level.getBlockEntity(pos));
        }
    }

    private static void migrateLiveBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        BlockState beforeState = level.getBlockState(pos);
        if (!rememberBlockEntityMaterials(level, blockEntity)) {
            return;
        }

        EnhancedSlabStorageOptimizer.optimizeAt(level, pos);
        BlockState afterState = level.getBlockState(pos);
        if (!(afterState.getBlock() instanceof EntityBlock)) {
            level.removeBlockEntity(pos);
        }
        level.sendBlockUpdated(pos, beforeState, afterState, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);
    }

    private static boolean rememberBlockEntityMaterials(ServerLevel level, BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();

        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab());
            return true;
        } else if (blockEntity instanceof StepBlockEntity step) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), step.getSlabState());
            return true;
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), verticalStep.getSlabState());
            return true;
        } else if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), quadStep.getAllSlabs());
            return true;
        } else if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), quadVerticalStep.getAllSlabs());
            return true;
        }

        return false;
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
