package org.onenonly.bitsandbalance.common.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
//? if mc == 1.21.1
import net.minecraft.core.HolderLookup;
//? endif
import net.minecraft.nbt.CompoundTag;
//? if mc != 1.21.1
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
//? if mc != 1.21.1
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
//? else
import org.jetbrains.annotations.Nullable;
//? endif
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;

import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//? if mc != 1.21.1
import java.util.function.Supplier;
//? endif
import java.util.function.BiConsumer;

public final class EnhancedSlabMaterialSavedData extends SavedData {
//? if mc == 1.21.1
    public static final String DATA_NAME = "bitsandbalance_enhanced_slab_materials";
//? else
    private static final String DATA_NAME = "bitsandbalance_enhanced_slab_materials";
//? endif
    private static final String TAG_ENTRIES = "entries";
    private static final int MAX_SLOTS = 4;
    private static final int CLIENT_STRIPE_COUNT = 64;
    private static final int CLIENT_STRIPE_MASK = CLIENT_STRIPE_COUNT - 1;
    private static final Map<String, ClientMaterialStore> CLIENT_MATERIALS_BY_DIMENSION = new ConcurrentHashMap<>();
    private static final Object CLIENT_WRITE_LOCK = new Object();
    private static volatile @Nullable String activeClientDimensionId;
    private static BiConsumer<Level, MaterialUpdate> syncCallback;
    private static final int MAX_PORTABLE_CHUNKS_PER_TICK = 64;
    private static final Map<ServerLevel, ConcurrentHashMap<Long, WeakReference<LevelChunk>>> PENDING_PORTABLE_CHUNKS = new ConcurrentHashMap<>();

    public record Entry(long pos, List<CompoundTag> slabs) {
    }

    public record MaterialUpdate(String dimensionId, long pos, BlockState[] slabs) {
    }

    /**
     * Immutable, allocation-free view of the four stored material slots. Block states are
     * immutable, and the backing slots are never exposed to callers.
     */
    public static final class MaterialView {
        private final @Nullable BlockState slot0;
        private final @Nullable BlockState slot1;
        private final @Nullable BlockState slot2;
        private final @Nullable BlockState slot3;

        private MaterialView(BlockState[] slabs) {
            this.slot0 = normalizedSlot(slabs, 0);
            this.slot1 = normalizedSlot(slabs, 1);
            this.slot2 = normalizedSlot(slabs, 2);
            this.slot3 = normalizedSlot(slabs, 3);
        }

        public @Nullable BlockState slot(int index) {
            return switch (index) {
                case 0 -> slot0;
                case 1 -> slot1;
                case 2 -> slot2;
                case 3 -> slot3;
                default -> null;
            };
        }

        public boolean isEmpty() {
            return slot0 == null && slot1 == null && slot2 == null && slot3 == null;
        }

        public boolean matches(@Nullable BlockState[] slabs) {
            return java.util.Objects.equals(slot0, rawSlot(slabs, 0))
                    && java.util.Objects.equals(slot1, rawSlot(slabs, 1))
                    && java.util.Objects.equals(slot2, rawSlot(slabs, 2))
                    && java.util.Objects.equals(slot3, rawSlot(slabs, 3));
        }

        public BlockState[] copySlots() {
            return new BlockState[]{slot0, slot1, slot2, slot3};
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MaterialView view
                    && java.util.Objects.equals(slot0, view.slot0)
                    && java.util.Objects.equals(slot1, view.slot1)
                    && java.util.Objects.equals(slot2, view.slot2)
                    && java.util.Objects.equals(slot3, view.slot3);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(slot0, slot1, slot2, slot3);
        }

        private static @Nullable BlockState normalizedSlot(@Nullable BlockState[] slabs, int index) {
            BlockState state = rawSlot(slabs, index);
            return state == null ? null : SlabStateTagHelper.normalizeStoredSlabState(state, Blocks.OAK_SLAB);
        }

        private static @Nullable BlockState rawSlot(@Nullable BlockState[] slabs, int index) {
            return slabs != null && index >= 0 && index < slabs.length ? slabs[index] : null;
        }
    }

    /**
     * Copy-on-write primitive maps keep render-worker reads lock-free. Client material writes are
     * rare and serialized; a point update copies only one stripe and a bulk packet copies each
     * affected stripe at most once before atomically publishing its new snapshot.
     */
    private static final class ClientMaterialStore {
        private final ClientMaterialStripe[] stripes = new ClientMaterialStripe[CLIENT_STRIPE_COUNT];

        private ClientMaterialStore() {
            for (int i = 0; i < stripes.length; i++) {
                stripes[i] = new ClientMaterialStripe();
            }
        }

        private @Nullable MaterialView get(long posLong) {
            return stripes[clientStripe(posLong)].snapshot.get(posLong);
        }

        private boolean update(long posLong, MaterialView stored) {
            ClientMaterialStripe stripe = stripes[clientStripe(posLong)];
            Long2ObjectOpenHashMap<MaterialView> previousSnapshot = stripe.snapshot;
            MaterialView previous = previousSnapshot.get(posLong);
            if (stored.isEmpty() ? previous == null : stored.equals(previous)) {
                return false;
            }
            Long2ObjectOpenHashMap<MaterialView> updated = new Long2ObjectOpenHashMap<>(previousSnapshot);
            if (stored.isEmpty()) {
                updated.remove(posLong);
            } else {
                updated.put(posLong, stored);
            }
            stripe.snapshot = updated;
            return true;
        }

        private LongOpenHashSet replaceChunkRange(int centerChunkX,
                                                  int centerChunkZ,
                                                  int chunkRadius,
                                                  long[] positions,
                                                  BlockState[][] slabs) {
            int count = Math.min(positions == null ? 0 : positions.length, slabs == null ? 0 : slabs.length);
            Long2ObjectOpenHashMap<MaterialView> incoming = new Long2ObjectOpenHashMap<>(Math.max(16, count * 2));
            for (int i = 0; i < count; i++) {
                incoming.put(positions[i], new MaterialView(slabs[i]));
            }

            @SuppressWarnings("unchecked")
            Long2ObjectOpenHashMap<MaterialView>[] updated =
                    (Long2ObjectOpenHashMap<MaterialView>[]) new Long2ObjectOpenHashMap<?>[CLIENT_STRIPE_COUNT];
            LongOpenHashSet changed = new LongOpenHashSet(Math.max(16, count));
            int radius = Math.max(0, chunkRadius);

            for (int stripeIndex = 0; stripeIndex < stripes.length; stripeIndex++) {
                Long2ObjectOpenHashMap<MaterialView> snapshot = stripes[stripeIndex].snapshot;
                for (var entry : snapshot.long2ObjectEntrySet()) {
                    long posLong = entry.getLongKey();
                    if (incoming.containsKey(posLong) || !insideChunkRange(posLong, centerChunkX, centerChunkZ, radius)) {
                        continue;
                    }
                    if (updated[stripeIndex] == null) {
                        updated[stripeIndex] = new Long2ObjectOpenHashMap<>(snapshot);
                    }
                    updated[stripeIndex].remove(posLong);
                    changed.add(posLong);
                }
            }

            for (var entry : incoming.long2ObjectEntrySet()) {
                long posLong = entry.getLongKey();
                MaterialView stored = entry.getValue();
                int stripeIndex = clientStripe(posLong);
                Long2ObjectOpenHashMap<MaterialView> snapshot = stripes[stripeIndex].snapshot;
                Long2ObjectOpenHashMap<MaterialView> target = updated[stripeIndex];
                MaterialView previous = target == null ? snapshot.get(posLong) : target.get(posLong);
                if (stored.isEmpty() ? previous == null : stored.equals(previous)) {
                    continue;
                }
                if (target == null) {
                    target = new Long2ObjectOpenHashMap<>(snapshot);
                    updated[stripeIndex] = target;
                }
                if (stored.isEmpty()) {
                    target.remove(posLong);
                } else {
                    target.put(posLong, stored);
                }
                changed.add(posLong);
            }

            for (int i = 0; i < updated.length; i++) {
                if (updated[i] != null) {
                    stripes[i].snapshot = updated[i];
                }
            }
            return changed;
        }

        private LongArrayList clearChunkRange(int centerChunkX,
                                              int centerChunkZ,
                                              int chunkRadius,
                                              @Nullable LongSet keepPositions) {
            LongArrayList removedPositions = new LongArrayList();
            int radius = Math.max(0, chunkRadius);
            for (ClientMaterialStripe stripe : stripes) {
                Long2ObjectOpenHashMap<MaterialView> snapshot = stripe.snapshot;
                Long2ObjectOpenHashMap<MaterialView> updated = null;
                for (var entry : snapshot.long2ObjectEntrySet()) {
                    long posLong = entry.getLongKey();
                    if ((keepPositions != null && keepPositions.contains(posLong))
                            || !insideChunkRange(posLong, centerChunkX, centerChunkZ, radius)) {
                        continue;
                    }
                    if (updated == null) {
                        updated = new Long2ObjectOpenHashMap<>(snapshot);
                    }
                    updated.remove(posLong);
                    removedPositions.add(posLong);
                }
                if (updated != null) {
                    stripe.snapshot = updated;
                }
            }
            return removedPositions;
        }

        private boolean isEmpty() {
            for (ClientMaterialStripe stripe : stripes) {
                if (!stripe.snapshot.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ClientMaterialStripe {
        private volatile Long2ObjectOpenHashMap<MaterialView> snapshot = new Long2ObjectOpenHashMap<>();
    }

    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
            CompoundTag.CODEC.listOf().optionalFieldOf("slabs", List.of()).forGetter(Entry::slabs)
    ).apply(instance, Entry::new));

    public static final Codec<EnhancedSlabMaterialSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRY_CODEC.listOf().optionalFieldOf(TAG_ENTRIES, List.of()).forGetter(EnhancedSlabMaterialSavedData::toEntries)
    ).apply(instance, EnhancedSlabMaterialSavedData::fromEntries));

//? if mc == 1.21.1
    public static final Factory<EnhancedSlabMaterialSavedData> FACTORY = new Factory<>(
            EnhancedSlabMaterialSavedData::new,
            EnhancedSlabMaterialSavedData::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
//? else
    public static final SavedDataType<EnhancedSlabMaterialSavedData> TYPE = createType();
//? endif

    private final Long2ObjectOpenHashMap<MaterialView> materialsByPos = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> positionsByChunk = new Long2ObjectOpenHashMap<>();

//? if mc != 1.21.1
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SavedDataType<EnhancedSlabMaterialSavedData> createType() {
        Supplier<EnhancedSlabMaterialSavedData> factory = EnhancedSlabMaterialSavedData::new;
        try {
            return (SavedDataType<EnhancedSlabMaterialSavedData>) SavedDataType.class
                    .getConstructor(String.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{DATA_NAME, factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return (SavedDataType<EnhancedSlabMaterialSavedData>) SavedDataType.class
                    .getConstructor(Identifier.class, java.util.function.Supplier.class, Codec.class, DataFixTypes.class)
                    .newInstance(new Object[]{Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, DATA_NAME), factory, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to construct enhanced slab material SavedDataType", exception);
        }
    }
//? endif

    public static EnhancedSlabMaterialSavedData get(ServerLevel level) {
//? if mc == 1.21.1
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
//? else
        return level.getDataStorage().computeIfAbsent(TYPE);
//? endif
    }

    public static @Nullable EnhancedSlabMaterialSavedData peek(ServerLevel level) {
//? if mc == 1.21.1
        return level.getDataStorage().get(FACTORY, DATA_NAME);
//? else
        return level.getDataStorage().get(TYPE);
//? endif
    }

//? if mc == 1.21.1
    public static EnhancedSlabMaterialSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result()
                .orElseGet(EnhancedSlabMaterialSavedData::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), this)
                .result()
                .orElseGet(CompoundTag::new);
    }
//? endif

    public static void remember(Level level, long posLong, BlockState... slabs) {
        if (level == null) {
            rememberClient(posLong, slabs);
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            MaterialView stored = new MaterialView(slabs);
            if (!get(serverLevel).setView(posLong, stored)) {
                return;
            }
            if (!stored.isEmpty()) {
                BlockPos pos = BlockPos.of(posLong);
                ensurePortableBlockEntity(serverLevel, pos);
                mirrorPortableBlockEntity(serverLevel, pos, stored);
            }
            if (syncCallback != null) {
                syncCallback.accept(level, new MaterialUpdate(dimensionId(level), posLong, stored.copySlots()));
            }
            org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFluidGeometryRefresh.afterStoredShapeChange(
                    serverLevel, BlockPos.of(posLong), serverLevel.getBlockState(BlockPos.of(posLong)));
        } else if (level.isClientSide()) {
            rememberClient(level, posLong, slabs);
        }
    }

    public static void setSyncCallback(@Nullable BiConsumer<Level, MaterialUpdate> callback) {
        syncCallback = callback;
    }

    public static void rememberClient(long posLong, BlockState... slabs) {
        rememberClient(activeOrFallbackClientDimension(), posLong, slabs);
    }

    public static void rememberClient(Level level, long posLong, BlockState... slabs) {
        if (updateClient(dimensionId(level), posLong, slabs)) {
            BlockPos pos = BlockPos.of(posLong);
            hydrateClientBlockEntityAt(level, pos);
            org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(level, pos);
            BlockState state = level.getBlockState(pos);
            // BE packets and local prediction can populate this store before the material packet.
            // Schedule the render update here so packet deduplication cannot swallow it later.
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    public static void rememberClient(@Nullable String dimensionId, long posLong, BlockState... slabs) {
        updateClient(dimensionId, posLong, slabs);
    }

    /** Applies a client material update atomically and reports whether the visible slots changed. */
    public static boolean updateClient(@Nullable String dimensionId, long posLong, BlockState... slabs) {
        String key = normalizeDimensionId(dimensionId);
        synchronized (CLIENT_WRITE_LOCK) {
            setActiveClientDimensionLocked(key);
            MaterialView stored = new MaterialView(slabs);
            ClientMaterialStore store = CLIENT_MATERIALS_BY_DIMENSION.get(key);
            if (store == null && stored.isEmpty()) {
                return false;
            }
            if (store == null) {
                store = new ClientMaterialStore();
                CLIENT_MATERIALS_BY_DIMENSION.put(key, store);
            }
            boolean changed = store.update(posLong, stored);
            if (stored.isEmpty() && store.isEmpty()) {
                CLIENT_MATERIALS_BY_DIMENSION.remove(key, store);
            }
            return changed;
        }
    }

    /** Replaces one authoritative client chunk range and returns every position whose slots changed. */
    public static LongOpenHashSet replaceClientChunkRange(@Nullable String dimensionId,
                                                           int centerChunkX,
                                                           int centerChunkZ,
                                                           int chunkRadius,
                                                           long[] positions,
                                                           BlockState[][] slabs) {
        String key = normalizeDimensionId(dimensionId);
        synchronized (CLIENT_WRITE_LOCK) {
            setActiveClientDimensionLocked(key);
            ClientMaterialStore store = CLIENT_MATERIALS_BY_DIMENSION.computeIfAbsent(
                    key, ignored -> new ClientMaterialStore());
            LongOpenHashSet changed = store.replaceChunkRange(
                    centerChunkX, centerChunkZ, chunkRadius, positions, slabs);
            if (store.isEmpty()) {
                CLIENT_MATERIALS_BY_DIMENSION.remove(key, store);
            }
            return changed;
        }
    }

    public static @Nullable BlockState[] getClient(long posLong) {
        MaterialView slots = getClientView(posLong);
        return slots == null ? null : slots.copySlots();
    }

    public static @Nullable BlockState[] getClient(Level level, long posLong) {
        return getClient(dimensionId(level), posLong);
    }

    public static @Nullable BlockState[] getClient(@Nullable String dimensionId, long posLong) {
        MaterialView slots = getClientView(dimensionId, posLong);
        return slots == null ? null : slots.copySlots();
    }

    public static @Nullable MaterialView getClientView(long posLong) {
        ClientMaterialStore active = CLIENT_MATERIALS_BY_DIMENSION.get(activeOrFallbackClientDimension());
        return active == null ? null : active.get(posLong);
    }

    public static @Nullable MaterialView getClientView(Level level, long posLong) {
        return getClientView(dimensionId(level), posLong);
    }

    public static @Nullable MaterialView getClientView(@Nullable String dimensionId, long posLong) {
        ClientMaterialStore store = CLIENT_MATERIALS_BY_DIMENSION.get(normalizeDimensionId(dimensionId));
        return store == null ? null : store.get(posLong);
    }

    public static @Nullable BlockState[] get(Level level, long posLong) {
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).get(posLong);
        }
        if (level != null && level.isClientSide()) {
            return getClient(level, posLong);
        }
        return null;
    }

    public static @Nullable MaterialView getView(Level level, long posLong) {
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel).getView(posLong);
        }
        if (level != null && level.isClientSide()) {
            return getClientView(level, posLong);
        }
        return null;
    }

    public static @Nullable BlockState[] peek(Level level, long posLong) {
        if (level instanceof ServerLevel serverLevel) {
            EnhancedSlabMaterialSavedData data = peek(serverLevel);
            return data == null ? null : data.get(posLong);
        }
        if (level != null && level.isClientSide()) {
            return getClient(level, posLong);
        }
        return null;
    }

    /** Rebuilds the optimized material index after a portable enhanced-slab BE is fully loaded. */
    public static void restoreFromBlockEntity(Level level, BlockEntity blockEntity) {
        if (level == null || blockEntity == null) {
            return;
        }
        long posLong = blockEntity.getBlockPos().asLong();
        if (level instanceof ServerLevel serverLevel) {
            EnhancedSlabMaterialSavedData existingData = peek(serverLevel);
            MaterialView existing = existingData == null ? null : existingData.getView(posLong);
            if (existing != null && !existing.isEmpty() && isPortableContainer(blockEntity.getBlockState())) {
                applyPortableView(blockEntity, existing);
                return;
            }
        }
        if (blockEntity instanceof MixedSlabBlockEntity mixed) {
            remember(level, posLong, mixed.getBottomSlab(), mixed.getTopSlab());
        } else if (blockEntity instanceof QuadStepBlockEntity quad) {
            remember(level, posLong, quad.getAllSlabs());
        } else if (blockEntity instanceof QuadVerticalStepBlockEntity quadVertical) {
            remember(level, posLong, quadVertical.getAllSlabs());
        } else if (blockEntity instanceof VerticalSlabBlockEntity vertical) {
            remember(level, posLong, vertical.getFacingSlab(), vertical.getOppositeSlab());
        } else if (blockEntity instanceof StepBlockEntity step) {
            remember(level, posLong, step.getSlabState());
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            remember(level, posLong, verticalStep.getSlabState());
        }
    }

    /** Client attachment is a read of the material index, never a publication of defaults. */
    public static void hydrateClientBlockEntity(Level level, BlockEntity blockEntity) {
        if (level == null || !level.isClientSide() || blockEntity == null) return;
        MaterialView stored = getClientView(level, blockEntity.getBlockPos().asLong());
        if (stored != null && !stored.isEmpty()) applyPortableView(blockEntity, stored);
    }

    /** Uses only an already attached entity, avoiding lazy creation during packet handling. */
    public static void hydrateClientBlockEntityAt(Level level, BlockPos pos) {
        if (level == null || !level.isClientSide() || !level.hasChunkAt(pos)) return;
        hydrateClientBlockEntity(level, level.getChunkAt(pos).getBlockEntities().get(pos));
    }

    /** Chunk-load callbacks must only enqueue: fetching the loading chunk can deadlock C2ME. */
    public static void queuePortableBlockEntities(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) {
            return;
        }
        BlockPos pos = chunk.getPos().getWorldPosition();
        long key = packChunk(pos.getX() >> 4, pos.getZ() >> 4);
        PENDING_PORTABLE_CHUNKS.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>())
                .put(key, new WeakReference<>(chunk));
    }

    /** Runs only on a later server tick and never requests or waits for a chunk to load. */
    public static void drainPortableBlockEntities(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int processed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            ConcurrentHashMap<Long, WeakReference<LevelChunk>> pending = PENDING_PORTABLE_CHUNKS.get(level);
            if (pending == null) {
                continue;
            }
            for (var entry : pending.entrySet()) {
                LevelChunk queued = entry.getValue().get();
                if (queued == null) {
                    pending.remove(entry.getKey(), entry.getValue());
                    continue;
                }
                int chunkX = (int) (long) entry.getKey();
                int chunkZ = (int) (entry.getKey() >> 32);
                LevelChunk ready = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (ready == null) {
                    continue;
                }
                if (!pending.remove(entry.getKey(), entry.getValue()) || ready != queued) {
                    continue;
                }
                ensurePortableBlockEntities(level, ready);
                for (BlockEntity blockEntity : new ArrayList<>(ready.getBlockEntities().values())) {
                    if (isPortableContainer(blockEntity.getBlockState())) {
                        restoreFromBlockEntity(level, blockEntity);
                    }
                }
                if (++processed >= MAX_PORTABLE_CHUNKS_PER_TICK) {
                    return;
                }
            }
        }
    }

    public static void clearPortableBlockEntities(MinecraftServer server) {
        PENDING_PORTABLE_CHUNKS.keySet().removeIf(level -> level.getServer() == server);
    }

    /**
     * Adds passive NBT mirrors to pre-fix chunks whose materials only existed in SavedData.
     * This runs once when a chunk loads and touches only the three compound container blocks.
     */
    public static int ensurePortableBlockEntities(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) {
            return 0;
        }
        EnhancedSlabMaterialSavedData data = peek(level);
        if (data == null) {
            return 0;
        }

        int createdCount = 0;
        String dimension = dimensionId(level);
        int chunkX;
        int chunkZ;
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        chunkX = chunk.getPos().x();
        chunkZ = chunk.getPos().z();
        //? else
        chunkX = chunk.getPos().x;
        chunkZ = chunk.getPos().z;
        //? endif
        for (MaterialUpdate update : data.snapshotAroundChunk(
                dimension, chunkX, chunkZ, 0)) {
            BlockPos pos = BlockPos.of(update.pos());
            BlockState ownerState = chunk.getBlockState(pos);
            if (!isPortableContainer(ownerState) || !(ownerState.getBlock() instanceof EntityBlock entityBlock)) {
                continue;
            }

            BlockEntity blockEntity = chunk.getBlockEntities().get(pos);
            if (blockEntity == null) {
                blockEntity = entityBlock.newBlockEntity(pos, ownerState);
                if (blockEntity == null) {
                    continue;
                }
                blockEntity.setLevel(level);
                chunk.setBlockEntity(blockEntity);
                createdCount++;
            }
            MaterialView portable = new MaterialView(update.slabs());
            mirrorPortableBlockEntity(level, pos, portable);
            // A newly attached BE starts with defaults and may briefly rebuild the index from
            // those defaults. Reassert the pre-migration snapshot after its level is attached.
            remember(level, update.pos(), portable.copySlots());
        }
        return createdCount;
    }

    public static void removeClient(long posLong) {
        removeClient(activeOrFallbackClientDimension(), posLong);
    }

    public static void removeClient(Level level, long posLong) {
        removeClient(dimensionId(level), posLong);
    }

    public static void removeClient(@Nullable String dimensionId, long posLong) {
        String key = normalizeDimensionId(dimensionId);
        synchronized (CLIENT_WRITE_LOCK) {
            ClientMaterialStore store = CLIENT_MATERIALS_BY_DIMENSION.get(key);
            if (store != null) {
                store.update(posLong, new MaterialView(null));
                if (store.isEmpty()) {
                    CLIENT_MATERIALS_BY_DIMENSION.remove(key, store);
                }
            }
        }
    }

    public static void setActiveClientDimension(@Nullable String dimensionId) {
        synchronized (CLIENT_WRITE_LOCK) {
            setActiveClientDimensionLocked(normalizeDimensionId(dimensionId));
        }
    }

    public static @Nullable String activeClientDimension() {
        return activeClientDimensionId;
    }

    public static void clearClient() {
        synchronized (CLIENT_WRITE_LOCK) {
            CLIENT_MATERIALS_BY_DIMENSION.clear();
            activeClientDimensionId = null;
        }
    }

    public static void clearClientChunkRange(@Nullable String dimensionId, int centerChunkX, int centerChunkZ, int chunkRadius) {
        clearClientChunkRangeExcept(dimensionId, centerChunkX, centerChunkZ, chunkRadius, null);
    }

    public static LongArrayList clearClientChunkRangeExcept(@Nullable String dimensionId, int centerChunkX, int centerChunkZ, int chunkRadius, @Nullable LongSet keepPositions) {
        String key = normalizeDimensionId(dimensionId);
        synchronized (CLIENT_WRITE_LOCK) {
            ClientMaterialStore store = CLIENT_MATERIALS_BY_DIMENSION.get(key);
            if (store == null) {
                return new LongArrayList();
            }
            LongArrayList removedPositions = store.clearChunkRange(
                    centerChunkX, centerChunkZ, chunkRadius, keepPositions);
            if (store.isEmpty()) {
                CLIENT_MATERIALS_BY_DIMENSION.remove(key, store);
            }
            return removedPositions;
        }
    }

    public static String dimensionId(Level level) {
        if (level == null) {
            return "";
        }
        try {
//? if mc == 1.21.1
            return level.dimension().location().toString();
//? else
            return level.dimension().identifier().toString();
//? endif
        } catch (Throwable ignored) {
            return "";
        }
    }

    public @Nullable BlockState[] get(long posLong) {
        MaterialView slots = getView(posLong);
        return slots == null ? null : slots.copySlots();
    }

    public void set(long posLong, BlockState... slabs) {
        setView(posLong, new MaterialView(slabs));
    }

    public @Nullable MaterialView getView(long posLong) {
        return materialsByPos.get(posLong);
    }

    private boolean setView(long posLong, MaterialView stored) {
        if (stored.isEmpty()) {
            if (materialsByPos.remove(posLong) == null) {
                return false;
            }
            removeFromChunkIndex(posLong);
            setDirty();
            return true;
        }
        MaterialView previous = materialsByPos.get(posLong);
        if (stored.equals(previous)) {
            return false;
        }
        materialsByPos.put(posLong, stored);
        if (previous == null) {
            positionsByChunk.computeIfAbsent(chunkKey(posLong), ignored -> new LongOpenHashSet()).add(posLong);
        }
        setDirty();
        return true;
    }

    private static void mirrorPortableBlockEntity(Level level, BlockPos pos, MaterialView stored) {
        if (level == null || pos == null || stored == null || !isPortableContainer(level.getBlockState(pos))) {
            return;
        }
        applyPortableView(level.getBlockEntity(pos), stored);
    }

    private static void ensurePortableBlockEntity(ServerLevel level, BlockPos pos) {
        BlockState ownerState = level.getBlockState(pos);
        if (!isPortableContainer(ownerState) || !(ownerState.getBlock() instanceof EntityBlock entityBlock)
                || level.getBlockEntity(pos) != null) {
            return;
        }
        BlockEntity created = entityBlock.newBlockEntity(pos, ownerState);
        if (created == null) {
            return;
        }
        created.setLevel(level);
        level.setBlockEntity(created);
    }

    private static void applyPortableView(@Nullable BlockEntity blockEntity, MaterialView stored) {
        if (blockEntity instanceof MixedSlabBlockEntity mixed) {
            BlockState[] current = {mixed.getBottomSlab(), mixed.getTopSlab()};
            if (!stored.matches(current)) {
                mixed.bitsandbalance$setSlabsFromStore(stored.slot(0), stored.slot(1));
            }
        } else if (blockEntity instanceof QuadStepBlockEntity quad) {
            if (!stored.matches(quad.getAllSlabs())) {
                quad.bitsandbalance$setSlabsFromStore(stored.copySlots());
            }
        } else if (blockEntity instanceof QuadVerticalStepBlockEntity quadVertical) {
            if (!stored.matches(quadVertical.getAllSlabs())) {
                quadVertical.bitsandbalance$setSlabsFromStore(stored.copySlots());
            }
        }
    }

    private static boolean isPortableContainer(BlockState state) {
        if (state == null) {
            return false;
        }
        return CommonBlocks.MIXED_SLAB != null && state.is(CommonBlocks.MIXED_SLAB)
                || CommonBlocks.QUAD_STEP != null && state.is(CommonBlocks.QUAD_STEP)
                || CommonBlocks.QUAD_VERTICAL_STEP != null && state.is(CommonBlocks.QUAD_VERTICAL_STEP);
    }

    public List<MaterialUpdate> snapshot(String dimensionId) {
        return snapshotAroundChunk(dimensionId, 0, 0, Integer.MAX_VALUE);
    }

    public List<MaterialUpdate> snapshotAroundChunk(String dimensionId, int centerChunkX, int centerChunkZ, int chunkRadius) {
        ArrayList<MaterialUpdate> entries = new ArrayList<>();
        String key = normalizeDimensionId(dimensionId);
        int radius = Math.max(0, chunkRadius);
        if (radius == Integer.MAX_VALUE) {
            entries.ensureCapacity(materialsByPos.size());
            for (var entry : materialsByPos.long2ObjectEntrySet()) {
                addSnapshotEntry(entries, key, entry.getLongKey(), entry.getValue());
            }
            return entries;
        }
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                LongOpenHashSet positions = positionsByChunk.get(packChunk(chunkX, chunkZ));
                if (positions == null) {
                    continue;
                }
                LongIterator iterator = positions.iterator();
                while (iterator.hasNext()) {
                    long posLong = iterator.nextLong();
                    addSnapshotEntry(entries, key, posLong, materialsByPos.get(posLong));
                }
            }
        }
        return entries;
    }

    private static EnhancedSlabMaterialSavedData fromEntries(List<Entry> entries) {
        EnhancedSlabMaterialSavedData data = new EnhancedSlabMaterialSavedData();
        if (entries == null) {
            return data;
        }
        for (Entry entry : entries) {
            if (entry == null || entry.slabs() == null || entry.slabs().isEmpty()) {
                continue;
            }
            BlockState[] slots = new BlockState[MAX_SLOTS];
            int max = Math.min(MAX_SLOTS, entry.slabs().size());
            for (int i = 0; i < max; i++) {
                CompoundTag tag = entry.slabs().get(i);
                if (tag != null && !tag.isEmpty()) {
                    slots[i] = SlabStateTagHelper.loadStoredSlabState(tag, Blocks.OAK_SLAB);
                }
            }
            if (!isEmpty(slots)) {
                data.putLoaded(entry.pos(), new MaterialView(slots));
            }
        }
        return data;
    }

    private List<Entry> toEntries() {
        List<Entry> entries = new ArrayList<>(materialsByPos.size());
        for (var entry : materialsByPos.long2ObjectEntrySet()) {
            MaterialView slots = entry.getValue();
            if (slots == null || slots.isEmpty()) {
                continue;
            }
            ArrayList<CompoundTag> tags = new ArrayList<>(MAX_SLOTS);
            for (int i = 0; i < MAX_SLOTS; i++) {
                BlockState state = slots.slot(i);
                tags.add(state == null ? new CompoundTag() : SlabStateTagHelper.saveSlabState(state, Blocks.OAK_SLAB));
            }
            entries.add(new Entry(entry.getLongKey(), tags));
        }
        return entries;
    }

    private void putLoaded(long posLong, MaterialView stored) {
        if (stored == null || stored.isEmpty()) {
            return;
        }
        materialsByPos.put(posLong, stored);
        positionsByChunk.computeIfAbsent(chunkKey(posLong), ignored -> new LongOpenHashSet()).add(posLong);
    }

    private void removeFromChunkIndex(long posLong) {
        long chunkKey = chunkKey(posLong);
        LongOpenHashSet positions = positionsByChunk.get(chunkKey);
        if (positions == null) {
            return;
        }
        positions.remove(posLong);
        if (positions.isEmpty()) {
            positionsByChunk.remove(chunkKey);
        }
    }

    private static void addSnapshotEntry(List<MaterialUpdate> entries, String dimensionId, long posLong, @Nullable MaterialView slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        entries.add(new MaterialUpdate(dimensionId, posLong, slots.copySlots()));
    }

    private static long chunkKey(long posLong) {
        BlockPos pos = BlockPos.of(posLong);
        return packChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return (chunkX & 0xffffffffL) | ((long) chunkZ << 32);
    }

    private static int clientStripe(long posLong) {
        long mixed = posLong ^ (posLong >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= mixed >>> 33;
        return (int) mixed & CLIENT_STRIPE_MASK;
    }

    private static boolean insideChunkRange(long posLong, int centerChunkX, int centerChunkZ, int radius) {
        BlockPos pos = BlockPos.of(posLong);
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return Math.abs(chunkX - centerChunkX) <= radius && Math.abs(chunkZ - centerChunkZ) <= radius;
    }

    private static boolean isEmpty(@Nullable BlockState[] slots) {
        if (slots == null) {
            return true;
        }
        for (BlockState slot : slots) {
            if (slot != null) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeDimensionId(@Nullable String dimensionId) {
        return dimensionId == null ? "" : dimensionId;
    }

    private static void setActiveClientDimensionLocked(String key) {
        if (activeClientDimensionId == null || activeClientDimensionId.equals(key)) {
            activeClientDimensionId = key;
            return;
        }
        CLIENT_MATERIALS_BY_DIMENSION.clear();
        activeClientDimensionId = key;
    }

    private static String activeOrFallbackClientDimension() {
        return activeClientDimensionId == null ? "" : activeClientDimensionId;
    }
}
