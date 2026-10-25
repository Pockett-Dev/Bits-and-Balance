package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnhancedSlabRenderModelCompat {
    private static final int MODEL_PARTS_CACHE_LIMIT = 8192;
    private static final int PART_LIST_CACHE_LIMIT = 8192;
    private static final int COMPOSITE_PART_LIST_CACHE_LIMIT = 8192;
    private static final int FILTERED_PART_CACHE_LIMIT = 16384;
    private static final int ALL_DIRECTION_MASK = directionMask(Direction.values());

    private static final Object MODEL_PARTS_CACHE_LOCK = new Object();
    private static final Object PART_LIST_CACHE_LOCK = new Object();
    private static final Object COMPOSITE_PART_LIST_CACHE_LOCK = new Object();
    private static final Object FILTERED_PART_CACHE_LOCK = new Object();

    private static final Map<ModelPartsKey, List<BlockModelPart>> MODEL_PARTS_CACHE = createBoundedCache(MODEL_PARTS_CACHE_LIMIT);
    private static final Map<BlockModelPart, List<BlockModelPart>> PART_LIST_CACHE = createBoundedCache(PART_LIST_CACHE_LIMIT);
    private static final Map<List<BlockModelPart>, List<BlockModelPart>> COMPOSITE_PART_LIST_CACHE = createBoundedCache(COMPOSITE_PART_LIST_CACHE_LIMIT);
    private static final Map<FilteredPartKey, BlockModelPart> FILTERED_PART_CACHE = createBoundedCache(FILTERED_PART_CACHE_LIMIT);

    private EnhancedSlabRenderModelCompat() {
    }

    public static void clearCaches() {
        synchronized (MODEL_PARTS_CACHE_LOCK) {
            MODEL_PARTS_CACHE.clear();
        }
        synchronized (PART_LIST_CACHE_LOCK) {
            PART_LIST_CACHE.clear();
        }
        synchronized (COMPOSITE_PART_LIST_CACHE_LOCK) {
            COMPOSITE_PART_LIST_CACHE.clear();
        }
        synchronized (FILTERED_PART_CACHE_LOCK) {
            FILTERED_PART_CACHE.clear();
        }
    }

    public static List<BlockModelPart> collectModelParts(BlockState sourceState, BlockPos pos) {
        if (sourceState == null) {
            return Collections.emptyList();
        }

        ModelPartsKey key = new ModelPartsKey(sourceState);
        synchronized (MODEL_PARTS_CACHE_LOCK) {
            List<BlockModelPart> cached = MODEL_PARTS_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(sourceState);
        List<BlockModelPart> parts = model.collectParts(RandomSource.create(bitsandbalance$modelSeed(sourceState)));
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockModelPart> immutableParts = List.copyOf(parts);
        synchronized (MODEL_PARTS_CACHE_LOCK) {
            MODEL_PARTS_CACHE.put(key, immutableParts);
        }
        return immutableParts;
    }

    public static List<BlockModelPart> singletonPartList(BlockModelPart part) {
        synchronized (PART_LIST_CACHE_LOCK) {
            List<BlockModelPart> cached = PART_LIST_CACHE.get(part);
            if (cached != null) {
                return cached;
            }

            List<BlockModelPart> created = Collections.singletonList(part);
            PART_LIST_CACHE.put(part, created);
            return created;
        }
    }

    public static List<BlockModelPart> partList(List<BlockModelPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        if (parts.size() == 1) {
            return singletonPartList(parts.getFirst());
        }

        synchronized (COMPOSITE_PART_LIST_CACHE_LOCK) {
            List<BlockModelPart> cached = COMPOSITE_PART_LIST_CACHE.get(parts);
            if (cached != null) {
                return cached;
            }

            List<BlockModelPart> created = List.copyOf(parts);
            COMPOSITE_PART_LIST_CACHE.put(created, created);
            return created;
        }
    }

    public static List<BlockModelPart> filterPartList(List<BlockModelPart> parts, int visibleMask) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        if (visibleMask == ALL_DIRECTION_MASK) {
            return parts;
        }

        ArrayList<BlockModelPart> filtered = new ArrayList<>(parts.size());
        for (BlockModelPart part : parts) {
            if (part == null) {
                continue;
            }
            filtered.add(filterDirections(part, visibleMask));
        }
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }
        return partList(filtered);
    }

    public static BlockModelPart filterDirections(BlockModelPart part, int visibleMask) {
        if (part == null || visibleMask == ALL_DIRECTION_MASK) {
            return part;
        }

        FilteredPartKey key = new FilteredPartKey(part, visibleMask);
        synchronized (FILTERED_PART_CACHE_LOCK) {
            BlockModelPart cached = FILTERED_PART_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        BlockModelPart filtered = new FilteredBlockModelPart(part, visibleMask);
        synchronized (FILTERED_PART_CACHE_LOCK) {
            FILTERED_PART_CACHE.put(key, filtered);
        }
        return filtered;
    }

    public static int directionMask(Direction... directions) {
        int mask = 0;
        if (directions == null) {
            return mask;
        }
        for (Direction direction : directions) {
            if (direction != null) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

    public static int allDirectionMask() {
        return ALL_DIRECTION_MASK;
    }

    public static int without(int mask, Direction direction) {
        return mask & ~(1 << direction.ordinal());
    }

    private static <K, V> Map<K, V> createBoundedCache(int limit) {
        return new LinkedHashMap<>(limit, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    private static long bitsandbalance$modelSeed(BlockState state) {
        return 0x9E3779B97F4A7C15L * Integer.toUnsignedLong(state.hashCode());
    }

    private record ModelPartsKey(BlockState state) {
    }

    private record FilteredPartKey(BlockModelPart part, int visibleMask) {
    }

    private static final class FilteredBlockModelPart implements BlockModelPart {
        private final BlockModelPart delegate;
        private final int visibleMask;
        private final EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        private List<BakedQuad> unculled;

        private FilteredBlockModelPart(BlockModelPart delegate, int visibleMask) {
            this.delegate = delegate;
            this.visibleMask = visibleMask;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction == null) {
                if (unculled == null) {
                    List<BakedQuad> source = delegate.getQuads(null);
                    ArrayList<BakedQuad> filtered = new ArrayList<>();
                    for (BakedQuad quad : source) {
                        if (quad != null && allows(quad.direction())) {
                            filtered.add(quad);
                        }
                    }
                    unculled = filtered.isEmpty() ? Collections.emptyList() : List.copyOf(filtered);
                }
                return unculled;
            }

            if (!allows(direction)) {
                return Collections.emptyList();
            }
            return byDirection.computeIfAbsent(direction, key -> {
                List<BakedQuad> quads = delegate.getQuads(key);
                return quads == null || quads.isEmpty() ? Collections.emptyList() : quads;
            });
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public net.minecraft.client.renderer.texture.TextureAtlasSprite particleIcon() {
            return delegate.particleIcon();
        }

        private boolean allows(Direction direction) {
            return (visibleMask & (1 << direction.ordinal())) != 0;
        }
    }
}
