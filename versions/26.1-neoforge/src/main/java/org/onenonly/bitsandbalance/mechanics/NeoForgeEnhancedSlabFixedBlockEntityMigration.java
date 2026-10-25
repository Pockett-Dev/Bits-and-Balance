package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.SlabStateTagHelper;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One-time bridge for worlds saved before fixed enhanced-slab variants stopped owning block entities.
 * Material data is captured before the legacy entity is rejected, then the encoded block ID is corrected
 * (or the generic storage block is restored) before the stale block entity is removed.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class NeoForgeEnhancedSlabFixedBlockEntityMigration {
    private static final int MAX_MIGRATIONS_PER_TICK = 512;
    private static final ArrayDeque<PendingMigration> PENDING = new ArrayDeque<>();
    private static final Set<PendingMigration> PENDING_KEYS = new HashSet<>();
    private static final Map<PendingMigration, BlockState[]> PENDING_MATERIALS = new HashMap<>();
    private static final Map<DeferredChunk, Map<PendingMigration, BlockState[]>> DEFERRED_BY_CHUNK = new HashMap<>();
    private static final Object QUEUE_LOCK = new Object();

    private NeoForgeEnhancedSlabFixedBlockEntityMigration() {
    }

    public static String diagnosticSummary() {
        synchronized (QUEUE_LOCK) {
            int deferred = DEFERRED_BY_CHUNK.values().stream().mapToInt(Map::size).sum();
            return "queued=" + PENDING.size() + ", deferred=" + deferred;
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        EnhancedSlabMaterialSavedData.queuePortableBlockEntities(level, chunk);
        resumeDeferred(level.dimension(), chunk.getPos().x(), chunk.getPos().z());
        for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
            migrateRejectedBlockEntity(level, blockEntity);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        drainQueue(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        EnhancedSlabMaterialSavedData.clearPortableBlockEntities(event.getServer());
        synchronized (QUEUE_LOCK) {
            PENDING.clear();
            PENDING_KEYS.clear();
            PENDING_MATERIALS.clear();
            DEFERRED_BY_CHUNK.clear();
        }
    }

    public static boolean migrateRejectedBlockEntity(ServerLevel level, BlockEntity blockEntity) {
        if (level == null || blockEntity == null) return false;
        BlockState state = blockEntity.getBlockState();
        if (!isFixedEnhancedSlab(state)) return false;
        return enqueue(level.dimension(), blockEntity.getBlockPos().asLong(),
                materialsFromBlockEntity(state, blockEntity));
    }

    public static boolean migrateRejectedBlockEntityTag(ServerLevel level, BlockPos pos, CompoundTag tag) {
        if (level == null || pos == null || tag == null) return false;
        BlockState state = level.getBlockState(pos);
        if (!isFixedEnhancedSlab(state)) return false;
        return enqueue(level.dimension(), pos.asLong(), materialsFromTag(state, tag));
    }

    private static boolean enqueue(ResourceKey<Level> dimension, long posLong, BlockState[] materials) {
        if (dimension == null || materials == null) return false;
        synchronized (QUEUE_LOCK) {
            PendingMigration pending = new PendingMigration(dimension, posLong);
            DeferredChunk deferredChunk = deferredChunk(pending);
            Map<PendingMigration, BlockState[]> deferred = DEFERRED_BY_CHUNK.get(deferredChunk);
            if (deferred != null) {
                deferred.remove(pending);
                if (deferred.isEmpty()) DEFERRED_BY_CHUNK.remove(deferredChunk);
            }
            PENDING_MATERIALS.put(pending, materials.clone());
            if (PENDING_KEYS.add(pending)) {
                PENDING.addLast(pending);
            }
        }
        return true;
    }

    private static void drainQueue(MinecraftServer server) {
        EnhancedSlabMaterialSavedData.drainPortableBlockEntities(server);
        if (server == null) return;
        for (int processed = 0; processed < MAX_MIGRATIONS_PER_TICK; processed++) {
            PendingMigration pending;
            BlockState[] materials;
            synchronized (QUEUE_LOCK) {
                pending = PENDING.pollFirst();
                if (pending == null) return;
                PENDING_KEYS.remove(pending);
                materials = PENDING_MATERIALS.remove(pending);
            }

            ServerLevel level = server.getLevel(pending.dimension());
            BlockPos pos = BlockPos.of(pending.posLong());
            if (level == null || !level.isLoaded(pos)) {
                if (materials != null) defer(pending, materials);
                continue;
            }
            migratePosition(level, pos, materials);
        }
    }

    private static void defer(PendingMigration pending, BlockState[] materials) {
        synchronized (QUEUE_LOCK) {
            DEFERRED_BY_CHUNK.computeIfAbsent(deferredChunk(pending), unused -> new HashMap<>())
                    .put(pending, materials.clone());
        }
    }

    private static void resumeDeferred(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        synchronized (QUEUE_LOCK) {
            Map<PendingMigration, BlockState[]> resumed = DEFERRED_BY_CHUNK.remove(new DeferredChunk(dimension, chunkX, chunkZ));
            if (resumed == null) return;
            for (var entry : resumed.entrySet()) {
                PendingMigration pending = entry.getKey();
                PENDING_MATERIALS.put(pending, entry.getValue());
                if (PENDING_KEYS.add(pending)) PENDING.addLast(pending);
            }
        }
    }

    private static DeferredChunk deferredChunk(PendingMigration pending) {
        BlockPos pos = BlockPos.of(pending.posLong());
        return new DeferredChunk(pending.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void migratePosition(ServerLevel level, BlockPos pos, @Nullable BlockState[] materials) {
        BlockState current = level.getBlockState(pos);
        if (!isFixedEnhancedSlab(current)) return;

        BlockState[] resolved = completeMaterials(current, materials);
        Block target = targetBlock(current, resolved);
        if (!(target instanceof StepBlock) && !(target instanceof VerticalSlabBlock)
                && !(target instanceof VerticalStepBlock)) {
            return;
        }

        BlockState replacement = copySharedProperties(current, target.defaultBlockState());
        boolean targetStoresMaterials = target instanceof EntityBlock;
        if (targetStoresMaterials) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), resolved);
        } else {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        }

        level.removeBlockEntity(pos);
        if (!replacement.equals(current)) {
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        }

        if (targetStoresMaterials) {
            installMaterials(level, pos, resolved);
        }
        BlockState after = level.getBlockState(pos);
        level.sendBlockUpdated(pos, current, after, Block.UPDATE_ALL);
        level.getChunkSource().blockChanged(pos);
    }

    private static Block targetBlock(BlockState state, BlockState[] materials) {
        if (state.getBlock() instanceof FixedStepBlock) {
            Block mapped = StepDynamicRegistry.getStepForSlab(materials[0].getBlock());
            return mapped != null ? mapped : CommonBlocks.STEP;
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock) {
            Block vertical = VerticalSlabDynamicRegistry.getVerticalForSlab(materials[0].getBlock());
            Block mapped = vertical == null ? null : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(vertical);
            return mapped != null ? mapped : CommonBlocks.VERTICAL_STEP;
        }

        BlockState opposite = materials.length > 1 ? materials[1] : null;
        boolean isDouble = state.hasProperty(VerticalSlabBlock.DOUBLE) && state.getValue(VerticalSlabBlock.DOUBLE);
        if (isDouble && opposite != null && opposite.getBlock() != materials[0].getBlock()) {
            return CommonBlocks.VERTICAL_SLAB;
        }
        Block mapped = VerticalSlabDynamicRegistry.getVerticalForSlab(materials[0].getBlock());
        return mapped != null ? mapped : CommonBlocks.VERTICAL_SLAB;
    }

    private static void installMaterials(ServerLevel level, BlockPos pos, BlockState[] materials) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof StepBlockEntity step) {
            step.setSlabState(materials[0]);
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            verticalStep.setSlabState(materials[0]);
        } else if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            BlockState opposite = materials.length > 1 && materials[1] != null ? materials[1] : materials[0];
            verticalSlab.setSlabs(materials[0], opposite);
        }
    }

    private static BlockState[] materialsFromBlockEntity(BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof StepBlockEntity step && step.getSlabState() != null) {
            return new BlockState[]{step.getSlabState()};
        }
        if (blockEntity instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
            return new BlockState[]{verticalStep.getSlabState()};
        }
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new BlockState[]{verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab()};
        }
        return completeMaterials(state, null);
    }

    private static BlockState[] materialsFromTag(BlockState state, CompoundTag tag) {
        if (state.getBlock() instanceof FixedStepBlock fixed) {
            return new BlockState[]{readSlab(tag, "Slab", fixed.getSourceSlab().defaultBlockState())};
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixed) {
            Block source = VerticalSlabDynamicRegistry.getSlabForVertical(fixed.getSourceVerticalSlab());
            BlockState fallback = (source != null ? source : fixed.getSourceVerticalSlab()).defaultBlockState();
            return new BlockState[]{readSlab(tag, "Slab", fallback)};
        }
        FixedVerticalSlabBlock fixed = (FixedVerticalSlabBlock) state.getBlock();
        BlockState fallback = fixed.getSourceSlab().defaultBlockState();
        return new BlockState[]{readSlab(tag, "FacingSlab", fallback), readSlab(tag, "OppositeSlab", fallback)};
    }

    private static BlockState readSlab(CompoundTag tag, String key, BlockState fallback) {
        CompoundTag child = tag.getCompoundOrEmpty(key);
        BlockState resolved = child.isEmpty() ? null
                : SlabStateTagHelper.loadOptionalStoredSlabState(child, fallback.getBlock());
        if (resolved == null) {
            String serialized = tag.getStringOr(key, "");
            try {
                Identifier id = Identifier.tryParse(serialized);
                Block block = id == null ? null : BuiltInRegistries.BLOCK.getValue(id);
                resolved = block == null ? null : block.defaultBlockState();
            } catch (Throwable ignored) {
            }
        }
        BlockState normalized = DynamicVariantMaterialHelper.normalizeSlabState(resolved);
        if (normalized == null) normalized = DynamicVariantMaterialHelper.normalizeSlabState(fallback);
        if (normalized == null) normalized = DynamicVariantMaterialHelper.normalizeSlabState(Blocks.OAK_SLAB.defaultBlockState());
        return normalized;
    }

    private static BlockState[] completeMaterials(BlockState state, @Nullable BlockState[] materials) {
        BlockState fallback;
        int size = 1;
        if (state.getBlock() instanceof FixedStepBlock fixed) {
            fallback = fixed.getSourceSlab().defaultBlockState();
        } else if (state.getBlock() instanceof FixedVerticalStepBlock fixed) {
            Block source = VerticalSlabDynamicRegistry.getSlabForVertical(fixed.getSourceVerticalSlab());
            fallback = (source != null ? source : fixed.getSourceVerticalSlab()).defaultBlockState();
        } else {
            fallback = ((FixedVerticalSlabBlock) state.getBlock()).getSourceSlab().defaultBlockState();
            size = 2;
        }
        BlockState normalizedFallback = DynamicVariantMaterialHelper.normalizeSlabState(fallback);
        if (normalizedFallback == null) {
            normalizedFallback = DynamicVariantMaterialHelper.normalizeSlabState(Blocks.OAK_SLAB.defaultBlockState());
        }
        BlockState[] completed = new BlockState[size];
        for (int i = 0; i < size; i++) {
            BlockState material = materials != null && i < materials.length ? materials[i] : null;
            BlockState normalized = DynamicVariantMaterialHelper.normalizeSlabState(material);
            completed[i] = normalized != null ? normalized : normalizedFallback;
        }
        return completed;
    }

    private static boolean isFixedEnhancedSlab(@Nullable BlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        return block instanceof FixedStepBlock
                || block instanceof FixedVerticalSlabBlock
                || block instanceof FixedVerticalStepBlock;
    }

    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) to = copySharedProperty(from, to, property);
        }
        return to;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copySharedProperty(BlockState from, BlockState to, Property property) {
        try {
            return to.setValue(property, from.getValue(property));
        } catch (IllegalArgumentException ignored) {
            return to;
        }
    }

    private record PendingMigration(ResourceKey<Level> dimension, long posLong) {
    }

    private record DeferredChunk(ResourceKey<Level> dimension, int x, int z) {
    }
}
