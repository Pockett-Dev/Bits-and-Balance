package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MovingBlockRenderTypeCompat {
    private static final int CACHE_LIMIT = 8192;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final BoundedConcurrentCache<Key, RenderType> CACHE = new BoundedConcurrentCache<>(CACHE_LIMIT);
    private static final Map<Class<?>, Optional<Method>> GET_QUADS_METHODS = new ConcurrentHashMap<>();

    private MovingBlockRenderTypeCompat() {
    }

    public static void clearCaches() {
        CACHE.clear();
        GET_QUADS_METHODS.clear();
    }

    public static RenderType get(BlockState blockState, BlockStateModelPart part) {
        return getCached(blockState, part);
    }

    public static RenderType get(BlockState blockState, List<?> parts) {
        return getCached(blockState, parts);
    }

    private static RenderType getCached(BlockState blockState, Object partsOrPart) {
        Key key = new Key(blockState, partsOrPart);
        RenderType cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        ChunkSectionLayer layer = partsOrPart instanceof List<?> parts ? findLayer(parts) : findLayer(partsOrPart);
        RenderType renderType = getForLayer(blockState, layer);
        RenderType raced = CACHE.putIfAbsent(key, renderType);
        return raced != null ? raced : renderType;
    }

    private static RenderType getForLayer(BlockState blockState, ChunkSectionLayer layer) {
        if (layer == ChunkSectionLayer.TRANSLUCENT || (layer == null && blockState != null && !blockState.getFluidState().isEmpty())) {
            return RenderTypes.translucentMovingBlock();
        }
        if (layer == ChunkSectionLayer.CUTOUT) {
            return RenderTypes.cutoutMovingBlock();
        }
        return RenderTypes.solidMovingBlock();
    }

    private static ChunkSectionLayer findLayer(List<?> parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        for (Object part : parts) {
            ChunkSectionLayer layer = findLayer(part);
            if (layer != null) {
                return layer;
            }
        }

        return null;
    }

    private static ChunkSectionLayer findLayer(Object part) {
        if (part == null) {
            return null;
        }
        if (part instanceof BlockStateModelPart blockStateModelPart) {
            return findLayer(blockStateModelPart);
        }

        try {
            Method getQuads = GET_QUADS_METHODS.computeIfAbsent(
                    part.getClass(),
                    type -> {
                        try {
                            return Optional.of(type.getMethod("getQuads", Direction.class));
                        } catch (NoSuchMethodException ignored) {
                            return Optional.empty();
                        }
                    }
            ).orElse(null);
            if (getQuads == null) {
                return null;
            }
            ChunkSectionLayer layer = firstLayer(getQuads.invoke(part, new Object[]{null}));
            if (layer != null) {
                return layer;
            }

            for (Direction direction : DIRECTIONS) {
                layer = firstLayer(getQuads.invoke(part, direction));
                if (layer != null) {
                    return layer;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static ChunkSectionLayer findLayer(BlockStateModelPart part) {
        if (part == null) {
            return null;
        }

        List<BakedQuad> all = part.getQuads(null);
        ChunkSectionLayer layer = firstLayer(all);
        if (layer != null) {
            return layer;
        }

        for (Direction direction : DIRECTIONS) {
            layer = firstLayer(part.getQuads(direction));
            if (layer != null) {
                return layer;
            }
        }

        return null;
    }

    private static ChunkSectionLayer firstLayer(Object quadsObject) {
        if (!(quadsObject instanceof List<?> quads) || quads.isEmpty()) {
            return null;
        }

        for (Object quadObject : quads) {
            if (quadObject instanceof BakedQuad quad && quad.materialInfo() != null) {
                return quad.materialInfo().layer();
            }
        }

        return null;
    }

    private static ChunkSectionLayer firstLayer(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        for (BakedQuad quad : quads) {
            if (quad != null && quad.materialInfo() != null) {
                return quad.materialInfo().layer();
            }
        }
        return null;
    }

    private static final class BoundedConcurrentCache<K, V> {
        private final int limit;
        private final ConcurrentHashMap<K, V> values = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<K> insertionOrder = new ConcurrentLinkedQueue<>();

        private BoundedConcurrentCache(int limit) {
            this.limit = limit;
        }

        private V get(K key) {
            return values.get(key);
        }

        private V putIfAbsent(K key, V value) {
            V existing = values.putIfAbsent(key, value);
            if (existing != null) {
                return existing;
            }
            insertionOrder.offer(key);
            while (values.size() > limit) {
                K eldest = insertionOrder.poll();
                if (eldest == null) {
                    break;
                }
                values.remove(eldest);
            }
            return null;
        }

        private void clear() {
            values.clear();
            insertionOrder.clear();
        }
    }

    private record Key(BlockState blockState, Object partsOrPart) {
    }
}
