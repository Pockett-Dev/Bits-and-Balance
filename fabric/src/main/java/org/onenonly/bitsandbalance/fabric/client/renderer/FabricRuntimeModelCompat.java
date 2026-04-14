package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.math.Quadrant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

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

    private static volatile Class<?> blockStateModelSetClass;
    private static volatile Class<?> blockStateModelClass;
    private static volatile Class<?> blockStateModelPartClass;
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
    private static volatile Constructor<?> bakedQuadCtor;

    private FabricRuntimeModelCompat() {
    }

    public static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to) {
        return buildCuboidPart(sourceState, pos, from, to, null);
    }

    public static @Nullable Object buildVerticalSlabPart(BlockState sourceState, BlockPos pos, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return null;
        }
        Vector3fc from = switch (half) {
            case SOUTH -> new org.joml.Vector3f(0.0f, 0.0f, 8.0f);
            case NORTH, WEST -> new org.joml.Vector3f(0.0f, 0.0f, 0.0f);
            case EAST -> new org.joml.Vector3f(8.0f, 0.0f, 0.0f);
            default -> new org.joml.Vector3f(0.0f, 0.0f, 0.0f);
        };
        Vector3fc to = switch (half) {
            case SOUTH, EAST -> new org.joml.Vector3f(16.0f, 16.0f, 16.0f);
            case NORTH -> new org.joml.Vector3f(16.0f, 16.0f, 8.0f);
            case WEST -> new org.joml.Vector3f(8.0f, 16.0f, 16.0f);
            default -> new org.joml.Vector3f(16.0f, 16.0f, 16.0f);
        };
        return buildCuboidPart(sourceState, pos, from, to, null);
    }

    public static @Nullable Object buildStepPart(BlockState sourceState, BlockPos pos, Direction facing, boolean top) {
        float yMin = top ? 8.0f : 0.0f;
        float yMax = top ? 16.0f : 8.0f;
        Vector3fc from = switch (facing) {
            case WEST -> new org.joml.Vector3f(0.0f, yMin, 0.0f);
            case SOUTH -> new org.joml.Vector3f(0.0f, yMin, 8.0f);
            case NORTH -> new org.joml.Vector3f(0.0f, yMin, 0.0f);
            case EAST -> new org.joml.Vector3f(8.0f, yMin, 0.0f);
            default -> new org.joml.Vector3f(8.0f, yMin, 0.0f);
        };
        Vector3fc to = switch (facing) {
            case WEST -> new org.joml.Vector3f(8.0f, yMax, 16.0f);
            case SOUTH -> new org.joml.Vector3f(16.0f, yMax, 16.0f);
            case NORTH -> new org.joml.Vector3f(16.0f, yMax, 8.0f);
            case EAST -> new org.joml.Vector3f(16.0f, yMax, 16.0f);
            default -> new org.joml.Vector3f(16.0f, yMax, 16.0f);
        };
        return buildCuboidPart(sourceState, pos, from, to, null);
    }

    public static @Nullable Object buildVerticalStepPart(BlockState sourceState, BlockPos pos, Direction facing, Direction side) {
        float xMin = side == Direction.EAST ? 8.0f : 0.0f;
        float xMax = side == Direction.EAST ? 16.0f : 8.0f;
        float zMin = facing == Direction.SOUTH ? 8.0f : 0.0f;
        float zMax = facing == Direction.SOUTH ? 16.0f : 8.0f;
        return buildCuboidPart(
                sourceState,
                pos,
                new org.joml.Vector3f(xMin, 0.0f, zMin),
                new org.joml.Vector3f(xMax, 16.0f, zMax),
                null
        );
    }

    public static @Nullable List<?> collectRuntimeModelParts(BlockState sourceState, @Nullable BlockPos pos) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            Method collectParts = getCollectPartsMethod(model.getClass());
            ArrayList<Object> parts = new ArrayList<>();
            long seed = pos != null ? pos.asLong() : 0L;
            collectParts.invoke(model, RandomSource.create(seed), parts);
            return parts.isEmpty() ? null : parts;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to,
                                                    @Nullable EnumMap<Direction, float[]> uvByDirection) {
        RuntimeFaceData faceData = extractFaceData(sourceState, pos);
        if (faceData == null || faceData.anyMaterialInfo == null) {
            return null;
        }

        EnumMap<Direction, List<Object>> byDirection = new EnumMap<>(Direction.class);
        ArrayList<Object> all = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            Object materialInfo = faceData.byDirection.getOrDefault(direction, faceData.anyMaterialInfo);
            float[] uv = uvByDirection != null ? uvByDirection.get(direction) : null;
            Object quad = bakeQuad(direction, from, to, materialInfo, uv);
            if (quad == null) {
                continue;
            }
            all.add(quad);
            byDirection.computeIfAbsent(direction, ignored -> new ArrayList<>()).add(quad);
        }

        if (all.isEmpty()) {
            return null;
        }

        return createPartProxy(all, byDirection, faceData.particleMaterial, faceData.materialFlags, faceData.useAmbientOcclusion);
    }

    private static @Nullable RuntimeSourceData extractSourceData(BlockState sourceState, BlockPos pos) {
        try {
            Object model = getRuntimeBlockStateModel(sourceState);
            if (model == null) {
                return null;
            }

            Method collectParts = getCollectPartsMethod(model.getClass());
            Method getQuads = getGetQuadsMethod();
            Method particleMaterial = getParticleMaterialMethod(model.getClass());
            Method materialFlags = getMaterialFlagsMethod(model.getClass());

            ArrayList<Object> parts = new ArrayList<>();
            collectParts.invoke(model, RandomSource.create(pos.asLong()), parts);
            if (parts.isEmpty()) {
                return null;
            }

            ArrayList<Object> sourceQuads = new ArrayList<>();
            for (Object part : parts) {
                if (part == null) {
                    continue;
                }
                boolean foundAll = false;
                try {
                    @SuppressWarnings("unchecked")
                    List<Object> allQuads = (List<Object>) getQuads.invoke(part, new Object[]{null});
                    if (allQuads != null && !allQuads.isEmpty()) {
                        sourceQuads.addAll(allQuads);
                        foundAll = true;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
                if (foundAll) {
                    continue;
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
            return new RuntimeSourceData(sourceQuads, particle, flags, true);
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

    private static Object createPartProxy(List<Object> all,
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
                        yield all;
                    }
                    List<Object> quads = byDirection.get(direction);
                    yield quads != null ? quads : Collections.emptyList();
                }
                case "useAmbientOcclusion" -> useAmbientOcclusion;
                case "particleMaterial" -> particleMaterial;
                case "materialFlags" -> materialFlags;
                case "toString" -> "FabricRuntimeModelCompatPartProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                default -> null;
            };
        };
        return Proxy.newProxyInstance(
                FabricRuntimeModelCompat.class.getClassLoader(),
                new Class<?>[]{getBlockStateModelPartClass()},
                handler
        );
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

    private static Class<?> getBlockStateModelPartClass() {
        Class<?> local = blockStateModelPartClass;
        if (local == null) {
            local = loadClass(CLASS_BLOCK_STATE_MODEL_PART);
            blockStateModelPartClass = local;
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
}
