package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.math.Quadrant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public final class FabricRuntimeModelCompat {
    private static final String CLASS_BLOCK_STATE_MODEL_SET = "net.minecraft.client.renderer.block.BlockStateModelSet";
    private static final String CLASS_BLOCK_STATE_MODEL = "net.minecraft.client.renderer.block.dispatch.BlockStateModel";
    private static final String CLASS_BLOCK_STATE_MODEL_PART = "net.minecraft.client.renderer.block.dispatch.BlockStateModelPart";
    private static final String CLASS_BAKED_QUAD = "net.minecraft.client.resources.model.geometry.BakedQuad";
    private static final String CLASS_BAKED_QUAD_MATERIAL_INFO = "net.minecraft.client.resources.model.geometry.BakedQuad$MaterialInfo";
    private static final String CLASS_FACE_BAKERY = "net.minecraft.client.resources.model.cuboid.FaceBakery";
    private static final String CLASS_CUBOID_FACE_UVS = "net.minecraft.client.resources.model.cuboid.CuboidFace$UVs";
    private static final String CLASS_BLOCK_MODEL_ROTATION = "net.minecraft.client.renderer.block.dispatch.BlockModelRotation";
    private static final String CLASS_MODEL_BAKER_INTERNER = "net.minecraft.client.resources.model.ModelBaker$Interner";
    private static final String CLASS_ITEM_TRANSFORM = "net.minecraft.client.resources.model.cuboid.ItemTransform";
    private static final String CLASS_FABRIC_BLOCK_STATE_MODEL_PART = "net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModelPart";
    private static final String CLASS_FABRIC_QUAD_EMITTER = "net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter";
    private static final String CLASS_FABRIC_VANILLA_BLOCK_MODEL_PART_ENCODER = "net.fabricmc.fabric.impl.client.renderer.VanillaBlockModelPartEncoder";

    private static volatile Class<?> blockStateModelSetClass;
    private static volatile Class<?> blockStateModelClass;
    private static volatile Class<?> blockStateModelPartClass;
    private static volatile Class<?> fabricBlockStateModelPartClass;
    private static volatile Class<?> fabricQuadEmitterClass;
    private static volatile Class<?> bakedQuadClass;
    private static volatile Class<?> bakedQuadMaterialInfoClass;
    private static volatile Class<?> cuboidFaceUvsClass;
    private static volatile Class<?> modelBakerInternerClass;
    private static volatile Method getBlockStateModelSetMethod;
    private static volatile Method getModelMethod;
    private static volatile Method collectPartsMethod;
    private static volatile Method getQuadsMethod;
    private static volatile Method quadMaterialInfoMethod;
    private static volatile Method runtimeQuadDirectionMethod;
    private static volatile Method particleMaterialMethod;
    private static volatile Method materialFlagsMethod;
    private static volatile Method bakeQuadMethod;
    private static volatile Field blockModelRotationIdentityField;
    private static volatile Object internerProxy;
    private static volatile Method quadDirectionMethod;
    private static volatile Method quadMaterialInfoMethodFromSource;
    private static volatile Method runtimeQuadPositionMethod;
    private static volatile Method runtimeQuadPackedUvMethod;
    private static volatile Method runtimeQuadSourceDirectionMethod;
    private static volatile Method runtimeQuadSourceMaterialInfoMethod;
    private static volatile Method fabricPartEmitQuadsMethod;
    private static volatile Constructor<?> bakedQuadCtor;
    private static final Predicate<Direction> NEVER_CULL = ignored -> false;

    private static final int PARTS_CACHE_LIMIT = 8192;
    private static final int CUBOID_CACHE_LIMIT = 8192;
    private static final int SOURCE_DATA_CACHE_LIMIT = 4096;
    private static final int PART_LIST_CACHE_LIMIT = 8192;
    private static final int COMPOSITE_PART_LIST_CACHE_LIMIT = 8192;
    private static final int FILTERED_PART_CACHE_LIMIT = 16384;
    private static final BoundedConcurrentCache<PartsKey, List<?>> PARTS_CACHE = new BoundedConcurrentCache<>(PARTS_CACHE_LIMIT);
    private static final BoundedConcurrentCache<CuboidKey, Object> CUBOID_CACHE = new BoundedConcurrentCache<>(CUBOID_CACHE_LIMIT);
    private static final BoundedConcurrentCache<SourceDataKey, RuntimeSourceData> SOURCE_DATA_CACHE = new BoundedConcurrentCache<>(SOURCE_DATA_CACHE_LIMIT);
    private static final BoundedConcurrentCache<BlockStateModelPart, List<BlockStateModelPart>> PART_LIST_CACHE = new BoundedConcurrentCache<>(PART_LIST_CACHE_LIMIT);
    private static final BoundedConcurrentCache<List<BlockStateModelPart>, List<BlockStateModelPart>> COMPOSITE_PART_LIST_CACHE = new BoundedConcurrentCache<>(COMPOSITE_PART_LIST_CACHE_LIMIT);
    private static final BoundedConcurrentCache<FilteredPartKey, BlockStateModelPart> FILTERED_PART_CACHE = new BoundedConcurrentCache<>(FILTERED_PART_CACHE_LIMIT);

    private FabricRuntimeModelCompat() {
    }

    public static void clearCaches() {
        PARTS_CACHE.clear();
        CUBOID_CACHE.clear();
        SOURCE_DATA_CACHE.clear();
        PART_LIST_CACHE.clear();
        COMPOSITE_PART_LIST_CACHE.clear();
        FILTERED_PART_CACHE.clear();
    }

    public static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to) {
        return buildCuboidPart(sourceState, pos, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), null);
    }

    public static @Nullable Object buildExactCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to, int visibleMask) {
        EnumMap<Direction, float[]> uvByDirection = null;
        return buildCuboidPart(sourceState, pos, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), uvByDirection, false, false, visibleMask, false);
    }

    public static float sourceModelHeight(BlockState sourceState, BlockPos pos) {
        try {
            RuntimeSourceData sourceData = extractSourceData(sourceState, pos);
            if (sourceData == null || sourceData.sourceQuads() == null || sourceData.sourceQuads().isEmpty()) {
                return 8.0f;
            }
            return computeSourceBounds(sourceData.sourceQuads()).height();
        } catch (ReflectiveOperationException ignored) {
            return 8.0f;
        }
    }

    public static @Nullable Object buildVerticalSlabPart(BlockState sourceState, BlockPos pos, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return null;
        }
        sourceState = verticalPrismRenderState(sourceState);
        float fromX;
        float fromZ;
        float toX;
        float toZ;
        switch (half) {
            case SOUTH -> {
                fromX = 0.0f;
                fromZ = 8.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
            case EAST -> {
                fromX = 8.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
            case NORTH -> {
                fromX = 0.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 8.0f;
            }
            case WEST -> {
                fromX = 0.0f;
                fromZ = 0.0f;
                toX = 8.0f;
                toZ = 16.0f;
            }
            default -> {
                fromX = 0.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
        };
        return buildCuboidPart(sourceState, pos, fromX, 0.0f, fromZ, toX, 16.0f, toZ, null, false, false, ALL_DIRECTION_MASK, false);
    }

    public static @Nullable Object buildStepPart(BlockState sourceState, BlockPos pos, Direction facing, boolean top) {
        float yMin = top ? 8.0f : 0.0f;
        float yMax = top ? 16.0f : 8.0f;
        float fromX;
        float fromZ;
        float toX;
        float toZ;
        switch (facing) {
            case WEST -> {
                fromX = 0.0f;
                fromZ = 0.0f;
                toX = 8.0f;
                toZ = 16.0f;
            }
            case SOUTH -> {
                fromX = 0.0f;
                fromZ = 8.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
            case NORTH -> {
                fromX = 0.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 8.0f;
            }
            case EAST -> {
                fromX = 8.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
            default -> {
                fromX = 8.0f;
                fromZ = 0.0f;
                toX = 16.0f;
                toZ = 16.0f;
            }
        };
        return buildCuboidPart(sourceState, pos, fromX, yMin, fromZ, toX, yMax, toZ, null);
    }

    public static @Nullable Object buildVerticalStepPart(BlockState sourceState, BlockPos pos, Direction facing, Direction side) {
        sourceState = verticalPrismRenderState(sourceState);
        float xMin = side == Direction.EAST ? 8.0f : 0.0f;
        float xMax = side == Direction.EAST ? 16.0f : 8.0f;
        float zMin = facing == Direction.SOUTH ? 8.0f : 0.0f;
        float zMax = facing == Direction.SOUTH ? 16.0f : 8.0f;
        return buildCuboidPart(sourceState, pos, xMin, 0.0f, zMin, xMax, 16.0f, zMax, null, false, false, ALL_DIRECTION_MASK, false);
    }

    private static BlockState verticalPrismRenderState(BlockState sourceState) {
        return SlabFamilyModelSourceSelector.verticalPrismRenderState(sourceState);
    }

    public static List<BlockStateModelPart> singletonPartList(BlockStateModelPart part) {
        List<BlockStateModelPart> cached = PART_LIST_CACHE.get(part);
        if (cached != null) {
            return cached;
        }
        List<BlockStateModelPart> created = Collections.singletonList(part);
        List<BlockStateModelPart> raced = PART_LIST_CACHE.putIfAbsent(part, created);
        return raced != null ? raced : created;
    }

    public static List<BlockStateModelPart> partList(List<BlockStateModelPart> parts) {
        if (parts.size() == 1) {
            return singletonPartList(parts.getFirst());
        }
        List<BlockStateModelPart> cached = COMPOSITE_PART_LIST_CACHE.get(parts);
        if (cached != null) {
            return cached;
        }
        List<BlockStateModelPart> created = List.copyOf(parts);
        List<BlockStateModelPart> raced = COMPOSITE_PART_LIST_CACHE.putIfAbsent(created, created);
        return raced != null ? raced : created;
    }

    public static List<BlockStateModelPart> filterPartList(List<?> parts, int visibleMask) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        if (visibleMask == ALL_DIRECTION_MASK) {
            boolean allBlockStateParts = true;
            for (Object part : parts) {
                if (!(part instanceof BlockStateModelPart)) {
                    allBlockStateParts = false;
                    break;
                }
            }
            if (allBlockStateParts) {
                @SuppressWarnings("unchecked")
                List<BlockStateModelPart> blockStateParts = (List<BlockStateModelPart>) parts;
                return blockStateParts;
            }
        }
        ArrayList<BlockStateModelPart> filtered = new ArrayList<>(parts.size());
        for (Object part : parts) {
            if (!(part instanceof BlockStateModelPart blockStateModelPart)) {
                continue;
            }
            filtered.add(filterDirections(blockStateModelPart, visibleMask));
        }
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }
        return partList(filtered);
    }

    @SuppressWarnings("unchecked")
    public static boolean emitFabricPartQuads(BlockStateModelPart part, Object emitter, Object cullTest) {
        if (part == null || emitter == null) {
            return false;
        }
        Predicate<Direction> cullPredicate = cullTest instanceof Predicate<?> predicate
                ? direction -> ((Predicate<Object>) predicate).test(direction)
                : NEVER_CULL;
        if (!hasVisibleQuads(part, cullPredicate)) {
            return false;
        }
        try {
            getFabricPartEmitQuadsMethod().invoke(null, part, emitter, cullPredicate);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean hasVisibleQuads(BlockStateModelPart part, Predicate<Direction> cullPredicate) {
        if (!cullPredicate.test(null) && !part.getQuads(null).isEmpty()) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (!cullPredicate.test(direction) && !part.getQuads(direction).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static BlockStateModelPart filterDirections(BlockStateModelPart part, int visibleMask) {
        if (part == null || visibleMask == ALL_DIRECTION_MASK) {
            return part;
        }
        FilteredPartKey key = new FilteredPartKey(part, visibleMask);
        BlockStateModelPart cached = FILTERED_PART_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BlockStateModelPart filtered = new FilteredBlockStateModelPart(part, visibleMask);
        BlockStateModelPart raced = FILTERED_PART_CACHE.putIfAbsent(key, filtered);
        return raced != null ? raced : filtered;
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

    public static @Nullable List<?> collectRuntimeModelParts(BlockState sourceState, @Nullable BlockPos pos) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            PartsKey key = new PartsKey(sourceState, pos != null ? pos.asLong() : 0L, model);
            List<?> cached = PARTS_CACHE.get(key);
            if (cached != null) {
                return cached;
            }

            Method collectParts = getCollectPartsMethod(model.getClass());
            ArrayList<Object> parts = new ArrayList<>();
            long seed = pos != null ? pos.asLong() : 0L;
            collectParts.invoke(model, RandomSource.create(seed), parts);
            if (parts.isEmpty()) {
                return null;
            }

            List<?> immutableParts = List.copyOf(parts);
            List<?> raced = PARTS_CACHE.putIfAbsent(key, immutableParts);
            return raced != null ? raced : immutableParts;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to,
                                                    @Nullable EnumMap<Direction, float[]> uvByDirection) {
        return buildCuboidPart(sourceState, pos, from.x(), from.y(), from.z(), to.x(), to.y(), to.z(), uvByDirection);
    }

    private static @Nullable Object buildCuboidPart(BlockState sourceState,
                                                    BlockPos pos,
                                                    float fromX,
                                                    float fromY,
                                                    float fromZ,
                                                    float toX,
                                                    float toY,
                                                    float toZ,
                                                    @Nullable EnumMap<Direction, float[]> uvByDirection) {
        return buildCuboidPart(sourceState, pos, fromX, fromY, fromZ, toX, toY, toZ, uvByDirection, true, true, ALL_DIRECTION_MASK, true);
    }

    private static @Nullable Object buildCuboidPart(BlockState sourceState,
                                                    BlockPos pos,
                                                    float fromX,
                                                    float fromY,
                                                    float fromZ,
                                                    float toX,
                                                    float toY,
                                                    float toZ,
                                                    @Nullable EnumMap<Direction, float[]> uvByDirection,
                                                    boolean adjustToSourceBounds,
                                                    boolean preserveSourceSideUv,
                                                    int visibleMask,
                                                    boolean useCache) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            CuboidKey key = new CuboidKey(sourceState, 0L, model, fromX, fromY, fromZ, toX, toY, toZ, UvKey.of(uvByDirection));
            if (useCache) {
                Object cached = CUBOID_CACHE.get(key);
                if (cached != null) {
                    return cached;
                }
            }

            RuntimeSourceData sourceData = extractSourceData(sourceState, null, model);
            if (sourceData == null || sourceData.sourceQuads() == null || sourceData.sourceQuads().isEmpty()) {
                return null;
            }

            Vector3fc from = new org.joml.Vector3f(fromX, fromY, fromZ);
            Vector3fc to = new org.joml.Vector3f(toX, toY, toZ);
            Map<Direction, List<Object>> sourceByDirection = mapQuadsByDirection(sourceData.sourceQuads());
            SourceBounds bounds = computeSourceBounds(sourceData.sourceQuads());
            Vector3fc adjustedFrom = adjustToSourceBounds ? adjustedFrom(from, to, bounds) : from;
            Vector3fc adjustedTo = adjustToSourceBounds ? adjustedTo(from, to, bounds) : to;
            Object fallbackQuad = sourceData.sourceQuads().getFirst();
            Method materialInfoMethod = getRuntimeQuadSourceMaterialInfoMethod();

            EnumMap<Direction, List<Object>> byDirection = new EnumMap<>(Direction.class);
            ArrayList<Object> unculled = new ArrayList<>(6);
            for (Direction direction : Direction.values()) {
                if ((visibleMask & (1 << direction.ordinal())) == 0) {
                    continue;
                }
                List<Object> templateQuads = sourceByDirection.get(direction);
                if (templateQuads == null || templateQuads.isEmpty()) {
                    templateQuads = Collections.singletonList(fallbackQuad);
                }
                float[] uv = uvByDirection != null ? uvByDirection.get(direction) : null;
                for (Object templateQuad : templateQuads) {
                    Object materialInfo = materialInfoMethod.invoke(templateQuad);
                    List<Object> quads = bakeQuads(direction, adjustedFrom, adjustedTo, materialInfo, templateQuad, uv, preserveSourceSideUv);
                    if (quads.isEmpty()) {
                        continue;
                    }
                    // Minecraft's direction cull test is only valid at the outer block boundary.
                    // A cut face at x/y/z=8 is exposed inside this block even if that direction has an opaque neighbor.
                    if (isBoundaryFace(direction, adjustedFrom, adjustedTo)) {
                        byDirection.computeIfAbsent(direction, ignored -> new ArrayList<>()).addAll(quads);
                    } else {
                        unculled.addAll(quads);
                    }
                }
            }

            if (unculled.isEmpty() && byDirection.isEmpty()) {
                return null;
            }

            Object part = createPartProxy(unculled, byDirection, sourceData.particleMaterial(), sourceData.materialFlags(), sourceData.useAmbientOcclusion());
            if (useCache) {
                Object raced = CUBOID_CACHE.putIfAbsent(key, part);
                return raced != null ? raced : part;
            }
            return part;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static SourceBounds computeSourceBounds(List<Object> quads) throws ReflectiveOperationException {
        SourceBounds bounds = new SourceBounds();
        Method positionMethod = getRuntimeQuadPositionMethod();
        for (Object quad : quads) {
            if (quad == null) {
                continue;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = (Vector3fc) positionMethod.invoke(quad, vertex);
                bounds.include(position.x() * 16.0f, position.y() * 16.0f, position.z() * 16.0f);
            }
        }
        return bounds;
    }

    private static boolean isBoundaryFace(Direction direction, Vector3fc from, Vector3fc to) {
        final float epsilon = 0.001F;
        return switch (direction) {
            case DOWN -> from.y() <= epsilon;
            case UP -> to.y() >= 16.0F - epsilon;
            case NORTH -> from.z() <= epsilon;
            case SOUTH -> to.z() >= 16.0F - epsilon;
            case WEST -> from.x() <= epsilon;
            case EAST -> to.x() >= 16.0F - epsilon;
        };
    }

    private static Vector3fc adjustedFrom(Vector3fc from, Vector3fc to, SourceBounds bounds) {
        float thickness = bounds.height();
        if (thickness <= 0.0f || Math.abs(thickness - 8.0f) < 0.001f) {
            return new org.joml.Vector3f(from);
        }

        org.joml.Vector3f adjusted = new org.joml.Vector3f(from);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        if (isHalfBlock(ySize) && !isHalfBlock(xSize) && !isHalfBlock(zSize)) {
            return adjusted;
        }
        if (isHalfBlock(xSize)) {
            adjusted.x = alignStart(from.x(), thickness);
        }
        if (isHalfBlock(zSize)) {
            adjusted.z = alignStart(from.z(), thickness);
        }
        return adjusted;
    }

    private static Vector3fc adjustedTo(Vector3fc from, Vector3fc to, SourceBounds bounds) {
        float thickness = bounds.height();
        if (thickness <= 0.0f || Math.abs(thickness - 8.0f) < 0.001f) {
            return new org.joml.Vector3f(to);
        }

        org.joml.Vector3f adjusted = new org.joml.Vector3f(to);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        if (isHalfBlock(ySize) && !isHalfBlock(xSize) && !isHalfBlock(zSize)) {
            adjusted.y = from.y() + thickness;
            return adjusted;
        }
        if (isHalfBlock(xSize)) {
            adjusted.x = alignEnd(from.x(), thickness);
        }
        if (isHalfBlock(zSize)) {
            adjusted.z = alignEnd(from.z(), thickness);
        }
        return adjusted;
    }

    private static boolean isHalfBlock(float size) {
        return Math.abs(size - 8.0f) < 0.001f;
    }

    private static float alignStart(float from, float thickness) {
        return from > 0.0f ? 16.0f - thickness : from;
    }

    private static float alignEnd(float from, float thickness) {
        return from > 0.0f ? 16.0f : from + thickness;
    }

    private static @Nullable RuntimeSourceData extractSourceData(BlockState sourceState, BlockPos pos) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            return extractSourceData(sourceState, pos, model);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable RuntimeSourceData extractSourceData(BlockState sourceState, BlockPos pos, Object model) {
        try {
            long seed = pos != null ? pos.asLong() : 0L;
            SourceDataKey key = new SourceDataKey(sourceState, seed, model);
            RuntimeSourceData cached = SOURCE_DATA_CACHE.get(key);
            if (cached != null) {
                return cached;
            }

            Method collectParts = getCollectPartsMethod(model.getClass());
            Method getQuads = getGetQuadsMethod();
            Method particleMaterial = getParticleMaterialMethod(model.getClass());
            Method materialFlags = getMaterialFlagsMethod(model.getClass());

            ArrayList<Object> parts = new ArrayList<>();
            collectParts.invoke(model, RandomSource.create(seed), parts);
            if (parts.isEmpty()) {
                return null;
            }

            ArrayList<Object> sourceQuads = new ArrayList<>();
            for (Object part : parts) {
                if (part == null) {
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> allQuads = (List<Object>) getQuads.invoke(part, new Object[]{null});
                    if (allQuads != null && !allQuads.isEmpty()) {
                        sourceQuads.addAll(allQuads);
                    }
                } catch (ReflectiveOperationException ignored) {
                }
                for (Direction direction : Direction.values()) {
                    @SuppressWarnings("unchecked")
                    List<Object> quads = (List<Object>) getQuads.invoke(part, direction);
                    if (quads != null && !quads.isEmpty()) {
                        sourceQuads.addAll(quads);
                    }
                }
            }

            if (sourceQuads.isEmpty()) {
                return null;
            }

            Object particle = particleMaterial.invoke(model);
            int flags = ((Number) materialFlags.invoke(model)).intValue();
            RuntimeSourceData sourceData = new RuntimeSourceData(List.copyOf(sourceQuads), particle, flags, true);
            RuntimeSourceData raced = SOURCE_DATA_CACHE.putIfAbsent(key, sourceData);
            return raced != null ? raced : sourceData;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Map<Direction, List<Object>> mapQuadsByDirection(List<Object> quads) {
        EnumMap<Direction, List<Object>> byDirection = new EnumMap<>(Direction.class);
        try {
            Method directionMethod = getRuntimeQuadDirectionMethod();
            for (Object quad : quads) {
                Direction direction = (Direction) directionMethod.invoke(quad);
                byDirection.computeIfAbsent(direction, ignored -> new ArrayList<>()).add(quad);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return byDirection;
    }

    private static List<Object> transformQuads(List<?> quads, int quarterTurns, float yOffset) {
        ArrayList<Object> out = new ArrayList<>(quads.size());
        for (Object quad : quads) {
            Object transformed = transformQuad(quad, quarterTurns & 3, yOffset);
            if (transformed != null) {
                out.add(transformed);
            }
        }
        return out;
    }

    private static @Nullable Object transformQuad(Object quad, int quarterTurns, float yOffset) {
        try {
            if (quarterTurns == 0 && yOffset == 0.0f) {
                return quad;
            }

            Method positionMethod = getRuntimeQuadPositionMethod();
            Method packedUvMethod = getRuntimeQuadPackedUvMethod();
            Method directionMethod = getRuntimeQuadDirectionMethod();
            Method materialInfoMethod = getRuntimeQuadSourceMaterialInfoMethod();

            Vector3fc[] moved = new Vector3fc[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                moved[vertex] = rotatePositionY((Vector3fc) positionMethod.invoke(quad, vertex), quarterTurns, yOffset);
            }

            return getBakedQuadConstructor().newInstance(
                    moved[0], moved[1], moved[2], moved[3],
                    ((Number) packedUvMethod.invoke(quad, 0)).longValue(),
                    ((Number) packedUvMethod.invoke(quad, 1)).longValue(),
                    ((Number) packedUvMethod.invoke(quad, 2)).longValue(),
                    ((Number) packedUvMethod.invoke(quad, 3)).longValue(),
                    rotateDirectionY((Direction) directionMethod.invoke(quad), quarterTurns),
                    materialInfoMethod.invoke(quad)
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Vector3fc rotatePositionY(Vector3fc position, int quarterTurns, float yOffset) {
        float x = position.x();
        float z = position.z();
        float rotatedX;
        float rotatedZ;
        switch (quarterTurns & 3) {
            case 1 -> {
                rotatedX = z;
                rotatedZ = 1.0f - x;
            }
            case 2 -> {
                rotatedX = 1.0f - x;
                rotatedZ = 1.0f - z;
            }
            case 3 -> {
                rotatedX = 1.0f - z;
                rotatedZ = x;
            }
            default -> {
                rotatedX = x;
                rotatedZ = z;
            }
        }
        return new org.joml.Vector3f(rotatedX, position.y() + yOffset, rotatedZ);
    }

    private static Direction rotateDirectionY(Direction direction, int quarterTurns) {
        if (direction == null || direction.getAxis() == Direction.Axis.Y || quarterTurns == 0) {
            return direction;
        }
        float x = direction.getStepX();
        float z = direction.getStepZ();
        float rotatedX;
        float rotatedZ;
        switch (quarterTurns & 3) {
            case 1 -> {
                rotatedX = z;
                rotatedZ = -x;
            }
            case 2 -> {
                rotatedX = -x;
                rotatedZ = -z;
            }
            case 3 -> {
                rotatedX = -z;
                rotatedZ = x;
            }
            default -> {
                rotatedX = x;
                rotatedZ = z;
            }
        }
        return Direction.getApproximateNearest(rotatedX, 0.0f, rotatedZ);
    }

    private static @Nullable RuntimeFaceData extractFaceData(BlockState sourceState, BlockPos pos) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            Method collectParts = getCollectPartsMethod(model.getClass());
            Method getQuads = getGetQuadsMethod();
            Method materialInfo = getQuadMaterialInfoMethod();
            Method particleMaterial = getParticleMaterialMethod(model.getClass());
            Method materialFlags = getMaterialFlagsMethod(model.getClass());

            ArrayList<Object> parts = new ArrayList<>();
            collectParts.invoke(model, RandomSource.create(pos.asLong()), parts);
            if (parts.isEmpty()) {
                return null;
            }

            EnumMap<Direction, Object> byDirection = new EnumMap<>(Direction.class);
            Object anyMaterialInfo = null;
            for (Object part : parts) {
                if (part == null) {
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked")
                    List<Object> allQuads = (List<Object>) getQuads.invoke(part, new Object[]{null});
                    if (allQuads != null && !allQuads.isEmpty()) {
                        for (Object quad : allQuads) {
                            if (quad == null) {
                                continue;
                            }
                            Method directionMethod = runtimeQuadDirectionMethod;
                            if (directionMethod == null) {
                                directionMethod = quad.getClass().getMethod("direction");
                                runtimeQuadDirectionMethod = directionMethod;
                            }
                            Direction direction = (Direction) directionMethod.invoke(quad);
                            if (direction == null || byDirection.containsKey(direction)) {
                                continue;
                            }
                            Object quadMaterialInfo = materialInfo.invoke(quad);
                            if (quadMaterialInfo == null) {
                                continue;
                            }
                            byDirection.put(direction, quadMaterialInfo);
                            if (anyMaterialInfo == null) {
                                anyMaterialInfo = quadMaterialInfo;
                            }
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                }

                for (Direction direction : Direction.values()) {
                    if (byDirection.containsKey(direction)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    List<Object> quads = (List<Object>) getQuads.invoke(part, direction);
                    if (quads == null || quads.isEmpty()) {
                        continue;
                    }
                    Object quad = quads.getFirst();
                    if (quad == null) {
                        continue;
                    }
                    Object quadMaterialInfo = materialInfo.invoke(quad);
                    if (quadMaterialInfo == null) {
                        continue;
                    }
                    byDirection.put(direction, quadMaterialInfo);
                    if (anyMaterialInfo == null) {
                        anyMaterialInfo = quadMaterialInfo;
                    }
                }
            }

            if (anyMaterialInfo == null) {
                return null;
            }

            Object particle = particleMaterial.invoke(model);
            int flags = ((Number) materialFlags.invoke(model)).intValue();
            return new RuntimeFaceData(byDirection, anyMaterialInfo, particle, flags, true);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object getRuntimeBlockStateModel(BlockState state) throws ReflectiveOperationException {
        Object modelManager = Minecraft.getInstance().getModelManager();
        if (modelManager == null) {
            return null;
        }
        Method getBlockStateModelSet = getBlockStateModelSetMethod(modelManager.getClass());
        Object modelSet = getBlockStateModelSet.invoke(modelManager);
        if (modelSet == null) {
            return null;
        }
        Method getModel = getModelMethod(modelSet.getClass());
        return getModel.invoke(modelSet, state);
    }

    public static @Nullable List<?> buildItemVerticalSlabPrism(List<?> sourceQuads) {
        EnumMap<Direction, Object> byDirection = extractFaceMaterialInfo(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<Object> out = new ArrayList<>(6);
        addQuad(out, Direction.NORTH, new float[]{0, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.SOUTH, new float[]{0, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.WEST, new float[]{0, 0, 8, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.EAST, new float[]{8, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.UP, new float[]{0, 8, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.DOWN, new float[]{0, 0, 16, 8}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        return out;
    }

    public static @Nullable List<?> buildItemStepPrism(List<?> sourceQuads) {
        EnumMap<Direction, Object> byDirection = extractFaceMaterialInfo(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<Object> out = new ArrayList<>(6);
        addQuad(out, Direction.NORTH, new float[]{0, 8, 8, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.SOUTH, new float[]{8, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.WEST, new float[]{0, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.EAST, new float[]{0, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.UP, new float[]{8, 0, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.DOWN, new float[]{8, 0, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        return out;
    }

    public static @Nullable List<?> buildItemVerticalStepPrism(List<?> sourceQuads) {
        EnumMap<Direction, Object> byDirection = extractFaceMaterialInfo(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<Object> out = new ArrayList<>(6);
        addQuad(out, Direction.NORTH, new float[]{8, 0, 16, 16}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        addQuad(out, Direction.SOUTH, new float[]{8, 0, 16, 16}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        addQuad(out, Direction.WEST, new float[]{0, 0, 8, 16}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        addQuad(out, Direction.EAST, new float[]{0, 0, 8, 16}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        addQuad(out, Direction.UP, new float[]{8, 0, 16, 8}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        addQuad(out, Direction.DOWN, new float[]{8, 0, 16, 8}, byDirection, VERTICAL_STEP_FROM, VERTICAL_STEP_TO);
        return out;
    }

    public static @Nullable Object createItemTransform(float rx, float ry, float rz,
                                                       float tx, float ty, float tz,
                                                       float sx, float sy, float sz) {
        try {
            return loadClass(CLASS_ITEM_TRANSFORM)
                    .getConstructor(Vector3fc.class, Vector3fc.class, Vector3fc.class)
                    .newInstance(new org.joml.Vector3f(rx, ry, rz), new org.joml.Vector3f(tx, ty, tz), new org.joml.Vector3f(sx, sy, sz));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void addQuad(List<Object> out, Direction direction, float[] uv, EnumMap<Direction, Object> byDirection, Vector3fc from, Vector3fc to) {
        Object materialInfo = byDirection.get(direction);
        if (materialInfo == null && !byDirection.isEmpty()) {
            materialInfo = byDirection.values().iterator().next();
        }
        Object quad = bakeQuad(direction, from, to, materialInfo, uv);
        if (quad != null) {
            out.add(quad);
        }
    }

    private static EnumMap<Direction, Object> extractFaceMaterialInfo(List<?> sourceQuads) {
        EnumMap<Direction, Object> byDirection = new EnumMap<>(Direction.class);
        if (sourceQuads == null || sourceQuads.isEmpty()) {
            return byDirection;
        }
        try {
            for (Object quad : sourceQuads) {
                if (quad == null) {
                    continue;
                }
                Method directionMethod = quadDirectionMethod;
                if (directionMethod == null) {
                    directionMethod = quad.getClass().getMethod("direction");
                    quadDirectionMethod = directionMethod;
                }
                Method materialInfoMethod = quadMaterialInfoMethodFromSource;
                if (materialInfoMethod == null) {
                    materialInfoMethod = quad.getClass().getMethod("materialInfo");
                    quadMaterialInfoMethodFromSource = materialInfoMethod;
                }
                Direction direction = (Direction) directionMethod.invoke(quad);
                Object materialInfo = materialInfoMethod.invoke(quad);
                if (direction != null && materialInfo != null) {
                    byDirection.putIfAbsent(direction, materialInfo);
                }
            }
            if (!byDirection.isEmpty()) {
                Object any = byDirection.values().iterator().next();
                for (Direction direction : Direction.values()) {
                    byDirection.putIfAbsent(direction, any);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return byDirection;
    }

    private static @Nullable Object bakeQuad(Direction direction, Vector3fc from, Vector3fc to, Object materialInfo, @Nullable float[] uvOverride) {
        try {
            float[] uv = uvOverride != null ? uvOverride : defaultUV(direction, from, to);
            Object uvs = getCuboidFaceUvsClass().getConstructor(float.class, float.class, float.class, float.class)
                    .newInstance(uv[0], uv[1], uv[2], uv[3]);
            return getBakeQuadMethod().invoke(
                    null,
                    getInternerProxy(),
                    from,
                    to,
                    uvs,
                    Quadrant.R0,
                    materialInfo,
                    direction,
                    getBlockModelRotationIdentity(),
                    null
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object createPartProxy(List<Object> unculled,
                                          Map<Direction, List<Object>> byDirection,
                                          Object particleMaterial,
                                          int materialFlags,
                                          boolean useAmbientOcclusion) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            return switch (name) {
                case "getQuads" -> {
                    Direction direction = args != null && args.length > 0 ? (Direction) args[0] : null;
                    if (direction == null) {
                        yield unculled;
                    }
                    List<Object> quads = byDirection.get(direction);
                    yield quads != null ? quads : Collections.emptyList();
                }
                case "useAmbientOcclusion" -> useAmbientOcclusion;
                case "particleMaterial" -> particleMaterial;
                case "materialFlags" -> materialFlags;
                case "emitQuads" -> {
                    Object emitter = args != null && args.length > 0 ? args[0] : null;
                    Object cullTest = args != null && args.length > 1 ? args[1] : null;
                    emitFabricPartQuads((BlockStateModelPart) proxy, emitter, cullTest);
                    yield null;
                }
                case "toString" -> "FabricRuntimeModelCompatPartProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                default -> null;
            };
        };
        Class<?> modelPartClass = getBlockStateModelPartClass();
        Class<?> fabricPartClass = getFabricBlockStateModelPartClassOrNull();
        Class<?>[] interfaces = fabricPartClass != null && !fabricPartClass.isAssignableFrom(modelPartClass)
                ? new Class<?>[]{modelPartClass, fabricPartClass}
                : new Class<?>[]{modelPartClass};
        return Proxy.newProxyInstance(
                FabricRuntimeModelCompat.class.getClassLoader(),
                interfaces,
                handler
        );
    }

    private static @Nullable float[] sourceSideUv(Direction direction, Vector3fc from, Vector3fc to, Object templateQuad, boolean preserveSourceSideUv) {
        if (!preserveSourceSideUv || direction == null || !direction.getAxis().isHorizontal() || !shouldPreserveSourceSideUv(from, to)) {
            return null;
        }
        float[] sourceUv = extractFaceUv(templateQuad);
        if (sourceUv == null) {
            return null;
        }
        float[] uv = defaultUV(direction, from, to);
        uv[1] = sourceUv[1];
        uv[3] = sourceUv[3];
        return uv;
    }

    private static List<Object> bakeQuads(Direction direction, Vector3fc from, Vector3fc to, Object materialInfo,
                                          Object templateQuad, @Nullable float[] uvOverride, boolean preserveSourceSideUv) {
        if (uvOverride == null && shouldRepeatSourceSideUv(direction, from, to, templateQuad, preserveSourceSideUv)) {
            float midY = from.y() + 8.0f;
            if (midY < to.y() - 0.001f) {
                float[] uv = sourceSideUv(direction, from, to, templateQuad, true);
                if (uv != null) {
                    ArrayList<Object> quads = new ArrayList<>(2);
                    Object lower = bakeQuad(direction, from, new org.joml.Vector3f(to.x(), midY, to.z()), materialInfo, uv);
                    Object upper = bakeQuad(direction, new org.joml.Vector3f(from.x(), midY, from.z()), to, materialInfo, uv);
                    if (lower != null) {
                        quads.add(lower);
                    }
                    if (upper != null) {
                        quads.add(upper);
                    }
                    return quads.isEmpty() ? Collections.emptyList() : quads;
                }
            }
        }

        float[] resolvedUv = uvOverride != null ? uvOverride : sourceSideUv(direction, from, to, templateQuad, preserveSourceSideUv);
        Object quad = bakeQuad(direction, from, to, materialInfo, resolvedUv);
        return quad == null ? Collections.emptyList() : Collections.singletonList(quad);
    }

    private static boolean shouldPreserveSourceSideUv(Vector3fc from, Vector3fc to) {
        return isHalfBlock(to.x() - from.x()) || isHalfBlock(to.y() - from.y()) || isHalfBlock(to.z() - from.z());
    }

    private static boolean shouldRepeatSourceSideUv(Direction direction, Vector3fc from, Vector3fc to, Object templateQuad, boolean preserveSourceSideUv) {
        float[] sourceUv = extractFaceUv(templateQuad);
        return preserveSourceSideUv
                && direction != null
                && direction.getAxis().isHorizontal()
                && templateQuad != null
                && sourceUv != null
                && isHalfBlock(sourceUv[3] - sourceUv[1])
                && !isHalfBlock(to.y() - from.y())
                && (isHalfBlock(to.x() - from.x()) || isHalfBlock(to.z() - from.z()));
    }

    private static @Nullable float[] extractFaceUv(Object quad) {
        if (quad == null) {
            return null;
        }
        try {
            Method packedUvMethod = getRuntimeQuadPackedUvMethod();
            Method materialInfoMethod = getRuntimeQuadSourceMaterialInfoMethod();
            Object materialInfo = materialInfoMethod.invoke(quad);
            if (materialInfo == null) {
                return null;
            }
            Object spriteObject = materialInfo.getClass().getMethod("sprite").invoke(materialInfo);
            if (!(spriteObject instanceof TextureAtlasSprite sprite)) {
                return null;
            }
            float uScale = sprite.getU1() - sprite.getU0();
            float vScale = sprite.getV1() - sprite.getV0();
            if (Math.abs(uScale) < 0.000001f || Math.abs(vScale) < 0.000001f) {
                return null;
            }

            float minU = Float.POSITIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                long packedUv = ((Number) packedUvMethod.invoke(quad, vertex)).longValue();
                float atlasU = Float.intBitsToFloat((int) (packedUv >>> 32));
                float atlasV = Float.intBitsToFloat((int) packedUv);
                float localU = 16.0f * (atlasU - sprite.getU0()) / uScale;
                float localV = 16.0f * (atlasV - sprite.getV0()) / vScale;
                minU = Math.min(minU, localU);
                minV = Math.min(minV, localV);
                maxU = Math.max(maxU, localU);
                maxV = Math.max(maxV, localV);
            }
            if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
                return null;
            }
            if (!saneUv(minU) || !saneUv(minV) || !saneUv(maxU) || !saneUv(maxV)) {
                return null;
            }
            return new float[]{minU, minV, maxU, maxV};
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean saneUv(float value) {
        return value >= -0.01f && value <= 16.01f;
    }

    private static float[] defaultUV(Direction dir, Vector3fc from, Vector3fc to) {
        float fx = from.x(), fy = from.y(), fz = from.z();
        float tx = to.x(), ty = to.y(), tz = to.z();
        return switch (dir) {
            case NORTH -> new float[]{16 - tx, 16 - ty, 16 - fx, 16 - fy};
            case SOUTH -> new float[]{fx, 16 - ty, tx, 16 - fy};
            case WEST -> new float[]{fz, 16 - ty, tz, 16 - fy};
            case EAST -> new float[]{16 - tz, 16 - ty, 16 - fz, 16 - fy};
            case UP -> new float[]{fx, fz, tx, tz};
            case DOWN -> new float[]{fx, 16 - tz, tx, 16 - fz};
        };
    }

    private static final Vector3fc VERTICAL_SLAB_FROM = new org.joml.Vector3f(0.0f, 0.0f, 8.0f);
    private static final Vector3fc VERTICAL_SLAB_TO = new org.joml.Vector3f(16.0f, 16.0f, 16.0f);
    private static final Vector3fc STEP_FROM = new org.joml.Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3fc STEP_TO = new org.joml.Vector3f(16.0f, 8.0f, 16.0f);
    private static final Vector3fc VERTICAL_STEP_FROM = new org.joml.Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3fc VERTICAL_STEP_TO = new org.joml.Vector3f(16.0f, 16.0f, 8.0f);
    private static final int ALL_DIRECTION_MASK = directionMask(Direction.values());

    private static Class<?> getBlockStateModelPartClass() {
        Class<?> local = blockStateModelPartClass;
        if (local == null) {
            local = loadClass(CLASS_BLOCK_STATE_MODEL_PART);
            blockStateModelPartClass = local;
        }
        return local;
    }

    private static @Nullable Class<?> getFabricBlockStateModelPartClassOrNull() {
        Class<?> local = fabricBlockStateModelPartClass;
        if (local != null) {
            return local;
        }
        try {
            local = Class.forName(CLASS_FABRIC_BLOCK_STATE_MODEL_PART);
            fabricBlockStateModelPartClass = local;
            return local;
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Class<?> getFabricQuadEmitterClass() {
        Class<?> local = fabricQuadEmitterClass;
        if (local == null) {
            local = loadClass(CLASS_FABRIC_QUAD_EMITTER);
            fabricQuadEmitterClass = local;
        }
        return local;
    }

    private static Class<?> getCuboidFaceUvsClass() {
        Class<?> local = cuboidFaceUvsClass;
        if (local == null) {
            local = loadClass(CLASS_CUBOID_FACE_UVS);
            cuboidFaceUvsClass = local;
        }
        return local;
    }

    private static Method getBlockStateModelSetMethod(Class<?> owner) throws ReflectiveOperationException {
        Method local = getBlockStateModelSetMethod;
        if (local == null || !local.getDeclaringClass().isAssignableFrom(owner)) {
            local = owner.getMethod("getBlockStateModelSet");
            getBlockStateModelSetMethod = local;
        }
        return local;
    }

    private static Method getModelMethod(Class<?> owner) throws ReflectiveOperationException {
        Method local = getModelMethod;
        if (local == null || !local.getDeclaringClass().isAssignableFrom(owner)) {
            local = owner.getMethod("get", BlockState.class);
            getModelMethod = local;
        }
        return local;
    }

    private static Method getCollectPartsMethod(Class<?> owner) throws ReflectiveOperationException {
        Method local = collectPartsMethod;
        if (local == null || !local.getDeclaringClass().isAssignableFrom(owner)) {
            local = owner.getMethod("collectParts", RandomSource.class, List.class);
            collectPartsMethod = local;
        }
        return local;
    }

    private static Method getGetQuadsMethod() throws ReflectiveOperationException {
        Method local = getQuadsMethod;
        if (local == null) {
            local = getBlockStateModelPartClass().getMethod("getQuads", Direction.class);
            getQuadsMethod = local;
        }
        return local;
    }

    private static Method getQuadMaterialInfoMethod() throws ReflectiveOperationException {
        Method local = quadMaterialInfoMethod;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getMethod("materialInfo");
            quadMaterialInfoMethod = local;
        }
        return local;
    }

    private static Method getRuntimeQuadPositionMethod() throws ReflectiveOperationException {
        Method local = runtimeQuadPositionMethod;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getMethod("position", int.class);
            runtimeQuadPositionMethod = local;
        }
        return local;
    }

    private static Method getRuntimeQuadPackedUvMethod() throws ReflectiveOperationException {
        Method local = runtimeQuadPackedUvMethod;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getMethod("packedUV", int.class);
            runtimeQuadPackedUvMethod = local;
        }
        return local;
    }

    private static Method getRuntimeQuadDirectionMethod() throws ReflectiveOperationException {
        Method local = runtimeQuadSourceDirectionMethod;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getMethod("direction");
            runtimeQuadSourceDirectionMethod = local;
        }
        return local;
    }

    private static Method getRuntimeQuadSourceMaterialInfoMethod() throws ReflectiveOperationException {
        Method local = runtimeQuadSourceMaterialInfoMethod;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getMethod("materialInfo");
            runtimeQuadSourceMaterialInfoMethod = local;
        }
        return local;
    }

    private static Constructor<?> getBakedQuadConstructor() throws ReflectiveOperationException {
        Constructor<?> local = bakedQuadCtor;
        if (local == null) {
            local = loadClass(CLASS_BAKED_QUAD).getConstructor(
                    org.joml.Vector3fc.class,
                    org.joml.Vector3fc.class,
                    org.joml.Vector3fc.class,
                    org.joml.Vector3fc.class,
                    long.class,
                    long.class,
                    long.class,
                    long.class,
                    Direction.class,
                    loadClass(CLASS_BAKED_QUAD_MATERIAL_INFO)
            );
            bakedQuadCtor = local;
        }
        return local;
    }

    private static Method getParticleMaterialMethod(Class<?> owner) throws ReflectiveOperationException {
        Method local = particleMaterialMethod;
        if (local == null || !local.getDeclaringClass().isAssignableFrom(owner)) {
            local = owner.getMethod("particleMaterial");
            particleMaterialMethod = local;
        }
        return local;
    }

    private static Method getMaterialFlagsMethod(Class<?> owner) throws ReflectiveOperationException {
        Method local = materialFlagsMethod;
        if (local == null || !local.getDeclaringClass().isAssignableFrom(owner)) {
            local = owner.getMethod("materialFlags");
            materialFlagsMethod = local;
        }
        return local;
    }

    private static Method getFabricPartEmitQuadsMethod() throws ReflectiveOperationException {
        Method local = fabricPartEmitQuadsMethod;
        if (local == null) {
            local = loadClass(CLASS_FABRIC_VANILLA_BLOCK_MODEL_PART_ENCODER).getMethod(
                    "emitQuads",
                    getBlockStateModelPartClass(),
                    getFabricQuadEmitterClass(),
                    Predicate.class
            );
            fabricPartEmitQuadsMethod = local;
        }
        return local;
    }

    private static Method getBakeQuadMethod() throws ReflectiveOperationException {
        Method local = bakeQuadMethod;
        if (local == null) {
            local = loadClass(CLASS_FACE_BAKERY).getMethod(
                    "bakeQuad",
                    loadClass(CLASS_MODEL_BAKER_INTERNER),
                    org.joml.Vector3fc.class,
                    org.joml.Vector3fc.class,
                    getCuboidFaceUvsClass(),
                    Quadrant.class,
                    loadClass(CLASS_BAKED_QUAD_MATERIAL_INFO),
                    Direction.class,
                    loadClass("net.minecraft.client.renderer.block.dispatch.ModelState"),
                    loadClass("net.minecraft.client.resources.model.cuboid.CuboidRotation")
            );
            bakeQuadMethod = local;
        }
        return local;
    }

    private static Object getBlockModelRotationIdentity() throws ReflectiveOperationException {
        Field local = blockModelRotationIdentityField;
        if (local == null) {
            local = loadClass(CLASS_BLOCK_MODEL_ROTATION).getField("IDENTITY");
            blockModelRotationIdentityField = local;
        }
        return local.get(null);
    }

    private static Object getInternerProxy() {
        Object local = internerProxy;
        if (local == null) {
            InvocationHandler handler = (proxy, method, args) -> {
                if (args == null || args.length == 0) {
                    return null;
                }
                return args[0];
            };
            local = Proxy.newProxyInstance(
                    FabricRuntimeModelCompat.class.getClassLoader(),
                    new Class<?>[]{loadClass(CLASS_MODEL_BAKER_INTERNER)},
                    handler
            );
            internerProxy = local;
        }
        return local;
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
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

    private record PartsKey(BlockState sourceState, long seed, Object model) {
    }

    private record SourceDataKey(BlockState sourceState, long seed, Object model) {
    }

    private record CuboidKey(BlockState sourceState,
                             long seed,
                             Object model,
                             float fromX,
                             float fromY,
                             float fromZ,
                             float toX,
                             float toY,
                             float toZ,
                             @Nullable UvKey uv) {
        private CuboidKey(BlockState sourceState, long seed, Object model, Vector3fc from, Vector3fc to,
                          @Nullable EnumMap<Direction, float[]> uvByDirection) {
            this(sourceState,
                    seed,
                    model,
                    from.x(),
                    from.y(),
                    from.z(),
                    to.x(),
                    to.y(),
                    to.z(),
                    UvKey.of(uvByDirection));
        }
    }

    private record UvKey(float north0, float north1, float north2, float north3,
                         float south0, float south1, float south2, float south3,
                         float west0, float west1, float west2, float west3,
                         float east0, float east1, float east2, float east3,
                         float up0, float up1, float up2, float up3,
                         float down0, float down1, float down2, float down3) {
        private static @Nullable UvKey of(@Nullable EnumMap<Direction, float[]> uvByDirection) {
            if (uvByDirection == null || uvByDirection.isEmpty()) {
                return null;
            }
            float[] north = uvOrEmpty(uvByDirection.get(Direction.NORTH));
            float[] south = uvOrEmpty(uvByDirection.get(Direction.SOUTH));
            float[] west = uvOrEmpty(uvByDirection.get(Direction.WEST));
            float[] east = uvOrEmpty(uvByDirection.get(Direction.EAST));
            float[] up = uvOrEmpty(uvByDirection.get(Direction.UP));
            float[] down = uvOrEmpty(uvByDirection.get(Direction.DOWN));
            return new UvKey(
                    north[0], north[1], north[2], north[3],
                    south[0], south[1], south[2], south[3],
                    west[0], west[1], west[2], west[3],
                    east[0], east[1], east[2], east[3],
                    up[0], up[1], up[2], up[3],
                    down[0], down[1], down[2], down[3]
            );
        }

        private static float[] uvOrEmpty(@Nullable float[] uv) {
            if (uv == null || uv.length < 4) {
                return new float[]{Float.NaN, Float.NaN, Float.NaN, Float.NaN};
            }
            return new float[]{uv[0], uv[1], uv[2], uv[3]};
        }
    }

    private record FilteredPartKey(BlockStateModelPart part, int visibleMask) {
    }

    private record RuntimeFaceData(EnumMap<Direction, Object> byDirection,
                                   Object anyMaterialInfo,
                                   Object particleMaterial,
                                   int materialFlags,
                                   boolean useAmbientOcclusion) {
    }

    private record RuntimeSourceData(List<Object> sourceQuads,
                                     Object particleMaterial,
                                     int materialFlags,
                                     boolean useAmbientOcclusion) {
    }

    private static final class FilteredBlockStateModelPart implements BlockStateModelPart {
        private final BlockStateModelPart delegate;
        private final int visibleMask;
        private final EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        private List<BakedQuad> unculled;

        private FilteredBlockStateModelPart(BlockStateModelPart delegate, int visibleMask) {
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
                        if (quad != null && bitsandbalance$allows(quad.direction())) {
                            filtered.add(quad);
                        }
                    }
                    unculled = filtered.isEmpty() ? Collections.emptyList() : List.copyOf(filtered);
                }
                return unculled;
            }
            if (!bitsandbalance$allows(direction)) {
                return Collections.emptyList();
            }
            return byDirection.computeIfAbsent(direction, key -> {
                List<BakedQuad> quads = delegate.getQuads(key);
                return quads.isEmpty() ? Collections.emptyList() : quads;
            });
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }

        private boolean bitsandbalance$allows(Direction direction) {
            return (visibleMask & (1 << direction.ordinal())) != 0;
        }
    }

    private static final class SourceBounds {
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        private void include(float x, float y, float z) {
            this.minY = Math.min(this.minY, y);
            this.maxY = Math.max(this.maxY, y);
        }

        private float height() {
            if (!Float.isFinite(this.minY) || !Float.isFinite(this.maxY)) {
                return 8.0f;
            }
            return Math.max(0.0f, this.maxY - this.minY);
        }
    }
}
