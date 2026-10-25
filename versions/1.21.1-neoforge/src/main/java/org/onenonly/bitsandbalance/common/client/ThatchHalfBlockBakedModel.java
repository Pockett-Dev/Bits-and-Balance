package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** A full-height half of Upgrade Aquatic's full thatch model, used by stacked Steps. */
public final class ThatchHalfBlockBakedModel extends BakedModelWrapper<BakedModel> {
    private static final Map<BakedModel, EnumMap<Direction, BakedModel>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BakedModel, BakedModel> FULL_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BakedModel, EnumMap<Direction, BakedModel>> SLAB_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final BlockState fullState;
    private final List<BakedQuad> quads;

    private ThatchHalfBlockBakedModel(BakedModel source, BlockState fullState, Direction half) {
        this(source, fullState, half, null);
    }

    private ThatchHalfBlockBakedModel(BakedModel source, BlockState fullState, Direction half, Direction hiddenFace) {
        super(source);
        this.fullState = fullState;
        List<BakedQuad> sourceQuads = new ArrayList<>();
        IdentityHashMap<BakedQuad, Boolean> seen = new IdentityHashMap<>();
        addQuads(sourceQuads, seen, source.getQuads(fullState, null, seededRandom()));
        for (Direction side : Direction.values()) {
            addQuads(sourceQuads, seen, source.getQuads(fullState, side, seededRandom()));
        }
        if (hiddenFace != null) {
            sourceQuads.removeIf(quad -> quad.getDirection() == hiddenFace);
        }

        // Crop once in the canonical east orientation, then rotate that complete textured mesh.
        // Directly compressing a different world axis for each half leaves directional UVs behind.
        List<BakedQuad> nativeVertical = half.getAxis().isHorizontal()
                ? ThatchModelGeometry.verticalSlabQuads(source) : null;
        EnhancedSlabItemBakedModel.GeometryTransform cropEastHalf;
        if (nativeVertical != null) {
            sourceQuads = nativeVertical;
            cropEastHalf = EnhancedSlabItemBakedModel.GeometryTransform.ROTATE_Y_COUNTERCLOCKWISE;
        } else {
            cropEastHalf = half.getAxis().isHorizontal()
                    ? EnhancedSlabItemBakedModel.GeometryTransform.resizePreservingOverhang(0.5F, 1.0F, 1.0F, 0.5F, 0.0F, 0.0F)
                    : EnhancedSlabItemBakedModel.GeometryTransform.IDENTITY;
        }
        EnhancedSlabItemBakedModel.GeometryTransform orientation = switch (half) {
            case SOUTH -> EnhancedSlabItemBakedModel.GeometryTransform.ROTATE_Y_CLOCKWISE;
            case WEST -> EnhancedSlabItemBakedModel.GeometryTransform.ROTATE_Y_HALF_TURN;
            case NORTH -> EnhancedSlabItemBakedModel.GeometryTransform.ROTATE_Y_COUNTERCLOCKWISE;
            case EAST -> EnhancedSlabItemBakedModel.GeometryTransform.IDENTITY;
            default -> EnhancedSlabItemBakedModel.GeometryTransform.IDENTITY;
        };
        List<BakedQuad> moved = new ArrayList<>(sourceQuads.size());
        for (BakedQuad quad : sourceQuads) {
            BakedQuad cropped = EnhancedSlabItemBakedModel.transformGeometryQuad(quad, cropEastHalf);
            moved.add(EnhancedSlabItemBakedModel.transformGeometryQuad(cropped, orientation));
        }
        this.quads = List.copyOf(moved);
    }

    public static BakedModel wrap(BakedModel source, BlockState fullState, Direction half) {
        if (source == null || fullState == null || half == null || half.getAxis() == Direction.Axis.Y) {
            return source;
        }
        synchronized (CACHE) {
            EnumMap<Direction, BakedModel> byHalf = CACHE.computeIfAbsent(source, unused -> new EnumMap<>(Direction.class));
            return byHalf.computeIfAbsent(half, unused -> new ThatchHalfBlockBakedModel(source, fullState, half));
        }
    }

    public static BakedModel wrapFull(BakedModel source, BlockState fullState) {
        if (source == null || fullState == null) {
            return source;
        }
        synchronized (FULL_CACHE) {
            return FULL_CACHE.computeIfAbsent(source,
                    unused -> new ThatchHalfBlockBakedModel(source, fullState, Direction.UP));
        }
    }

    /** Preserve the native top/bottom slab, including its fringe, inside mixed materials. */
    public static BakedModel wrapSlab(BakedModel source, BlockState slabState, Direction hiddenFace) {
        synchronized (SLAB_CACHE) {
            EnumMap<Direction, BakedModel> byFace = SLAB_CACHE.computeIfAbsent(source,
                    unused -> new EnumMap<>(Direction.class));
            Direction key = hiddenFace == null ? Direction.NORTH : hiddenFace;
            return byFace.computeIfAbsent(key,
                    unused -> new ThatchHalfBlockBakedModel(source, slabState, Direction.UP, hiddenFace));
        }
    }

    public static BlockState fullStateForStackedPair(BlockState[] slots, int bottomIndex) {
        if (slots == null || slots.length < 4 || bottomIndex < 0 || bottomIndex > 1) {
            return null;
        }
        return SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                slots[bottomIndex], slots[bottomIndex + 2]);
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
        synchronized (FULL_CACHE) {
            FULL_CACHE.clear();
        }
        synchronized (SLAB_CACHE) {
            SLAB_CACHE.clear();
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return side == null ? this.quads : List.of();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData modelData, RenderType renderType) {
        return side == null ? this.quads : List.of();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        return this.originalModel.getRenderTypes(this.fullState, random, ModelData.EMPTY);
    }

    private static void addQuads(List<BakedQuad> target, IdentityHashMap<BakedQuad, Boolean> seen, List<BakedQuad> source) {
        if (source == null) {
            return;
        }
        for (BakedQuad quad : source) {
            if (quad != null && seen.put(quad, Boolean.TRUE) == null) {
                target.add(quad);
            }
        }
    }

    private static RandomSource seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }
}
