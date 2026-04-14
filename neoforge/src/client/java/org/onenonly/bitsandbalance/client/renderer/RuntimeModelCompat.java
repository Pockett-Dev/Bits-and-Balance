package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.math.Quadrant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class RuntimeModelCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-runtime-model");
    private static final Set<Direction> LOGGED_FALLBACK_DIRECTIONS = Collections.synchronizedSet(EnumSet.noneOf(Direction.class));
    private static volatile Object internerProxy;

    private static final Vector3fc VERTICAL_SLAB_FROM = new Vector3f(0.0f, 0.0f, 8.0f);
    private static final Vector3fc VERTICAL_SLAB_TO = new Vector3f(16.0f, 16.0f, 16.0f);
    private static final Vector3fc STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3fc STEP_TO = new Vector3f(16.0f, 8.0f, 16.0f);
    private static final Vector3fc VERTICAL_STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3fc VERTICAL_STEP_TO = new Vector3f(16.0f, 16.0f, 8.0f);

    private RuntimeModelCompat() {
    }

    public static @Nullable List<BlockStateModelPart> collectRuntimeModelParts(BlockState sourceState, @Nullable BlockPos pos) {
        try {
            BlockPos seedPos = pos != null ? pos : BlockPos.ZERO;
            var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(sourceState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(seedPos.asLong()), parts);
            return parts.isEmpty() ? null : parts;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to) {
        return buildCuboidPart(sourceState, pos, from, to, null);
    }

    public static @Nullable Object buildVerticalSlabPart(BlockState sourceState, BlockPos pos, Direction half) {
        if (half == null || half.getAxis() == Direction.Axis.Y) {
            return null;
        }
        Vector3fc from = switch (half) {
            case SOUTH -> new Vector3f(0.0f, 0.0f, 8.0f);
            case NORTH, WEST -> new Vector3f(0.0f, 0.0f, 0.0f);
            case EAST -> new Vector3f(8.0f, 0.0f, 0.0f);
            default -> new Vector3f(0.0f, 0.0f, 0.0f);
        };
        Vector3fc to = switch (half) {
            case SOUTH, EAST -> new Vector3f(16.0f, 16.0f, 16.0f);
            case NORTH -> new Vector3f(16.0f, 16.0f, 8.0f);
            case WEST -> new Vector3f(8.0f, 16.0f, 16.0f);
            default -> new Vector3f(16.0f, 16.0f, 16.0f);
        };
        return buildCuboidPart(sourceState, pos, from, to, null);
    }

    public static @Nullable Object buildStepPart(BlockState sourceState, BlockPos pos, Direction facing, boolean top) {
        float yMin = top ? 8.0f : 0.0f;
        float yMax = top ? 16.0f : 8.0f;
        Vector3fc from = switch (facing) {
            case WEST -> new Vector3f(0.0f, yMin, 0.0f);
            case SOUTH -> new Vector3f(0.0f, yMin, 8.0f);
            case NORTH -> new Vector3f(0.0f, yMin, 0.0f);
            case EAST -> new Vector3f(8.0f, yMin, 0.0f);
            default -> new Vector3f(8.0f, yMin, 0.0f);
        };
        Vector3fc to = switch (facing) {
            case WEST -> new Vector3f(8.0f, yMax, 16.0f);
            case SOUTH -> new Vector3f(16.0f, yMax, 16.0f);
            case NORTH -> new Vector3f(16.0f, yMax, 8.0f);
            case EAST -> new Vector3f(16.0f, yMax, 16.0f);
            default -> new Vector3f(16.0f, yMax, 16.0f);
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
                new Vector3f(xMin, 0.0f, zMin),
                new Vector3f(xMax, 16.0f, zMax),
                null
        );
    }

    private static @Nullable Object buildCuboidPart(BlockState sourceState, BlockPos pos, Vector3fc from, Vector3fc to,
                                                    @Nullable EnumMap<Direction, float[]> uvByDirection) {
        RuntimeFaceData faceData = extractFaceData(sourceState, pos);
        if (faceData == null || faceData.anyQuad == null) {
            return null;
        }

        EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        ArrayList<BakedQuad> all = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            BakedQuad templateQuad = faceData.byDirection.getOrDefault(direction, faceData.anyQuad);
            float[] uv = uvByDirection != null ? uvByDirection.get(direction) : null;
            BakedQuad quad = bakeQuad(direction, from, to, templateQuad, uv);
            if (quad == null) {
                continue;
            }
            all.add(quad);
            byDirection.computeIfAbsent(direction, ignored -> new ArrayList<>()).add(quad);
        }

        if (all.isEmpty()) {
            return null;
        }

        return new RuntimeModelPart(all, byDirection, faceData.particleMaterial, faceData.materialFlags, faceData.useAmbientOcclusion);
    }

    public static @Nullable List<?> buildItemVerticalSlabPrism(List<?> sourceQuads) {
        EnumMap<Direction, BakedQuad> byDirection = extractFaceQuads(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<BakedQuad> out = new ArrayList<>(6);
        addQuad(out, Direction.NORTH, new float[]{0, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.SOUTH, new float[]{0, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.WEST, new float[]{0, 0, 8, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.EAST, new float[]{8, 0, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.UP, new float[]{0, 8, 16, 16}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        addQuad(out, Direction.DOWN, new float[]{0, 0, 16, 8}, byDirection, VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO);
        return out;
    }

    public static @Nullable List<?> buildItemStepPrism(List<?> sourceQuads) {
        EnumMap<Direction, BakedQuad> byDirection = extractFaceQuads(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<BakedQuad> out = new ArrayList<>(6);
        addQuad(out, Direction.NORTH, new float[]{0, 8, 8, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.SOUTH, new float[]{8, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.WEST, new float[]{0, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.EAST, new float[]{0, 8, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.UP, new float[]{8, 0, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        addQuad(out, Direction.DOWN, new float[]{8, 0, 16, 16}, byDirection, STEP_FROM, STEP_TO);
        return out;
    }

    public static @Nullable List<?> buildItemVerticalStepPrism(List<?> sourceQuads) {
        EnumMap<Direction, BakedQuad> byDirection = extractFaceQuads(sourceQuads);
        if (byDirection.isEmpty()) {
            return null;
        }
        ArrayList<BakedQuad> out = new ArrayList<>(6);
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
        return new ItemTransform(
                new Vector3f(rx, ry, rz),
                new Vector3f(tx, ty, tz),
                new Vector3f(sx, sy, sz)
        );
    }

    private static @Nullable RuntimeFaceData extractFaceData(BlockState sourceState, BlockPos pos) {
        try {
            var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(sourceState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(pos.asLong()), parts);
            if (parts == null || parts.isEmpty()) {
                return null;
            }

            EnumMap<Direction, BakedQuad> byDirection = new EnumMap<>(Direction.class);
            BakedQuad anyQuad = null;
            for (BlockStateModelPart part : parts) {
                if (part == null) {
                    continue;
                }

                for (Direction direction : Direction.values()) {
                    if (byDirection.containsKey(direction)) {
                        continue;
                    }
                    List<BakedQuad> quads = part.getQuads(direction);
                    if (quads == null || quads.isEmpty()) {
                        continue;
                    }
                    BakedQuad quad = quads.getFirst();
                    if (quad == null) {
                        continue;
                    }
                    byDirection.put(direction, quad);
                    if (anyQuad == null) {
                        anyQuad = quad;
                    }
                }

                if (anyQuad == null) {
                    try {
                        List<BakedQuad> allQuads = part.getQuads(null);
                        if (allQuads == null || allQuads.isEmpty()) {
                            continue;
                        }
                        for (BakedQuad quad : allQuads) {
                            if (quad != null) {
                                anyQuad = quad;
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            if (anyQuad == null) {
                return null;
            }

            Material.Baked particleMaterial = model.particleMaterial();
            int materialFlags = model.materialFlags();
            return new RuntimeFaceData(byDirection, anyQuad, particleMaterial, materialFlags, true);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable RuntimeSourceData extractSourceData(BlockState sourceState, BlockPos pos) {
        try {
            var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(sourceState);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(pos.asLong()), parts);
            if (parts == null || parts.isEmpty()) {
                return null;
            }

            ArrayList<BakedQuad> sourceQuads = new ArrayList<>();
            for (BlockStateModelPart part : parts) {
                if (part == null) {
                    continue;
                }
                boolean foundAll = false;
                try {
                    List<BakedQuad> allQuads = part.getQuads(null);
                    if (allQuads != null && !allQuads.isEmpty()) {
                        sourceQuads.addAll(allQuads);
                        foundAll = true;
                    }
                } catch (Throwable ignored) {
                }
                if (foundAll) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    List<BakedQuad> quads = part.getQuads(direction);
                    if (quads != null && !quads.isEmpty()) {
                        sourceQuads.addAll(quads);
                    }
                }
            }

            if (sourceQuads.isEmpty()) {
                return null;
            }

            return new RuntimeSourceData(sourceQuads, model.particleMaterial(), model.materialFlags(), true);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable List<BakedQuad> castQuads(@Nullable List<?> quads) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        ArrayList<BakedQuad> out = new ArrayList<>(quads.size());
        for (Object quad : quads) {
            if (quad instanceof BakedQuad bakedQuad) {
                out.add(bakedQuad);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static Object createRuntimeModelPart(List<BakedQuad> quads, RuntimeSourceData sourceData) {
        EnumMap<Direction, List<BakedQuad>> byDirection = new EnumMap<>(Direction.class);
        for (BakedQuad quad : quads) {
            byDirection.computeIfAbsent(quad.direction(), ignored -> new ArrayList<>()).add(quad);
        }
        return new RuntimeModelPart(quads, byDirection, sourceData.particleMaterial(), sourceData.materialFlags(), sourceData.useAmbientOcclusion());
    }

    private static List<BakedQuad> transformQuads(List<BakedQuad> quads, int quarterTurns, float yOffset) {
        ArrayList<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(transformQuad(quad, quarterTurns & 3, yOffset));
        }
        return out;
    }

    private static BakedQuad transformQuad(BakedQuad quad, int quarterTurns, float yOffset) {
        if (quarterTurns == 0 && yOffset == 0.0f) {
            return quad;
        }

        Vector3fc[] moved = new Vector3fc[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            moved[vertex] = rotatePositionY(quad.position(vertex), quarterTurns, yOffset);
        }

        return new BakedQuad(
                moved[0], moved[1], moved[2], moved[3],
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                rotateDirectionY(quad.direction(), quarterTurns),
                quad.materialInfo()
        );
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
        return new Vector3f(rotatedX, position.y() + yOffset, rotatedZ);
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

    private static void addQuad(List<BakedQuad> out,
                                Direction direction,
                                float[] uv,
                                EnumMap<Direction, BakedQuad> byDirection,
                                Vector3fc from,
                                Vector3fc to) {
        BakedQuad templateQuad = byDirection.get(direction);
        if (templateQuad == null && !byDirection.isEmpty()) {
            templateQuad = byDirection.values().iterator().next();
        }
        BakedQuad quad = bakeQuad(direction, from, to, templateQuad, uv);
        if (quad != null) {
            out.add(quad);
        }
    }

    private static EnumMap<Direction, BakedQuad> extractFaceQuads(List<?> sourceQuads) {
        EnumMap<Direction, BakedQuad> byDirection = new EnumMap<>(Direction.class);
        if (sourceQuads == null || sourceQuads.isEmpty()) {
            return byDirection;
        }
        for (Object sourceQuad : sourceQuads) {
            if (!(sourceQuad instanceof BakedQuad quad)) {
                continue;
            }
            Direction direction = quad.direction();
            if (direction != null && !byDirection.containsKey(direction)) {
                byDirection.put(direction, quad);
            }
        }
        return byDirection;
    }

    private static @Nullable BakedQuad bakeQuad(Direction direction,
                                                Vector3fc from,
                                                Vector3fc to,
                                                @Nullable BakedQuad templateQuad,
                                                @Nullable float[] uv) {
        if (templateQuad == null) {
            return null;
        }

        float[] resolvedUv = uv != null ? uv : defaultUv(direction, from, to);
        try {
            return FaceBakery.bakeQuad(
                getInternerProxy(),
                from,
                to,
                new CuboidFace.UVs(resolvedUv[0], resolvedUv[1], resolvedUv[2], resolvedUv[3]),
                Quadrant.R0,
                templateQuad.materialInfo(),
                direction,
                BlockModelRotation.IDENTITY,
                null
        );
        } catch (Throwable throwable) {
            if (LOGGED_FALLBACK_DIRECTIONS.add(direction)) {
                LOGGER.warn("Falling back to manual NeoForge quad bake for direction {}", direction, throwable);
            }
            return bakeQuadManual(direction, from, to, templateQuad, resolvedUv);
        }
    }

    private static BakedQuad bakeQuadManual(Direction direction,
                                            Vector3fc from,
                                            Vector3fc to,
                                            BakedQuad templateQuad,
                                            float[] uv) {
        Vector3fc[] positions = quadPositions(direction, from, to);
        return new BakedQuad(
                positions[0], positions[1], positions[2], positions[3],
                packUv(uv[2], uv[3]),
                packUv(uv[2], uv[1]),
                packUv(uv[0], uv[1]),
                packUv(uv[0], uv[3]),
                direction,
                templateQuad.materialInfo(),
                templateQuad.bakedNormals(),
                templateQuad.bakedColors()
        );
    }

    private static Vector3fc[] quadPositions(Direction direction, Vector3fc from, Vector3fc to) {
        float fx = from.x() / 16.0f;
        float fy = from.y() / 16.0f;
        float fz = from.z() / 16.0f;
        float tx = to.x() / 16.0f;
        float ty = to.y() / 16.0f;
        float tz = to.z() / 16.0f;
        return switch (direction) {
            case NORTH -> new Vector3fc[]{
                    new Vector3f(tx, fy, fz),
                    new Vector3f(tx, ty, fz),
                    new Vector3f(fx, ty, fz),
                    new Vector3f(fx, fy, fz)
            };
            case SOUTH -> new Vector3fc[]{
                    new Vector3f(fx, fy, tz),
                    new Vector3f(fx, ty, tz),
                    new Vector3f(tx, ty, tz),
                    new Vector3f(tx, fy, tz)
            };
            case WEST -> new Vector3fc[]{
                    new Vector3f(fx, fy, fz),
                    new Vector3f(fx, ty, fz),
                    new Vector3f(fx, ty, tz),
                    new Vector3f(fx, fy, tz)
            };
            case EAST -> new Vector3fc[]{
                    new Vector3f(tx, fy, tz),
                    new Vector3f(tx, ty, tz),
                    new Vector3f(tx, ty, fz),
                    new Vector3f(tx, fy, fz)
            };
            case UP -> new Vector3fc[]{
                    new Vector3f(fx, ty, fz),
                    new Vector3f(fx, ty, tz),
                    new Vector3f(tx, ty, tz),
                    new Vector3f(tx, ty, fz)
            };
            case DOWN -> new Vector3fc[]{
                    new Vector3f(fx, fy, tz),
                    new Vector3f(fx, fy, fz),
                    new Vector3f(tx, fy, fz),
                    new Vector3f(tx, fy, tz)
            };
        };
    }

    private static long packUv(float u, float v) {
        return (long) Float.floatToRawIntBits(u) & 0xffffffffL | ((long) Float.floatToRawIntBits(v) & 0xffffffffL) << 32;
    }

    private static float[] defaultUv(Direction direction, Vector3fc from, Vector3fc to) {
        return switch (direction) {
            case NORTH -> new float[]{16.0f - to.x(), 16.0f - to.y(), 16.0f - from.x(), 16.0f - from.y()};
            case SOUTH -> new float[]{from.x(), 16.0f - to.y(), to.x(), 16.0f - from.y()};
            case WEST -> new float[]{from.z(), 16.0f - to.y(), to.z(), 16.0f - from.y()};
            case EAST -> new float[]{16.0f - to.z(), 16.0f - to.y(), 16.0f - from.z(), 16.0f - from.y()};
            case UP -> new float[]{from.x(), from.z(), to.x(), to.z()};
            case DOWN -> new float[]{from.x(), 16.0f - to.z(), to.x(), 16.0f - from.z()};
        };
    }

    private static ModelBaker.Interner getInternerProxy() {
        Object proxy = internerProxy;
        if (proxy == null) {
            InvocationHandler handler = (ignoredProxy, method, args) -> args != null && args.length > 0 ? args[0] : null;
            proxy = Proxy.newProxyInstance(
                    RuntimeModelCompat.class.getClassLoader(),
                    new Class<?>[]{ModelBaker.Interner.class},
                    handler
            );
            internerProxy = proxy;
        }
        return (ModelBaker.Interner) proxy;
    }

    private record RuntimeFaceData(
            EnumMap<Direction, BakedQuad> byDirection,
            BakedQuad anyQuad,
            Material.Baked particleMaterial,
            int materialFlags,
            boolean useAmbientOcclusion
    ) {
    }

        private record RuntimeSourceData(
            List<BakedQuad> sourceQuads,
            Material.Baked particleMaterial,
            int materialFlags,
            boolean useAmbientOcclusion
        ) {
        }

    private static final class RuntimeModelPart implements BlockStateModelPart {
        private final List<BakedQuad> all;
        private final EnumMap<Direction, List<BakedQuad>> byDirection;
        private final Material.Baked particleMaterial;
        private final int materialFlags;
        private final boolean useAmbientOcclusion;

        private RuntimeModelPart(List<BakedQuad> all,
                                 EnumMap<Direction, List<BakedQuad>> byDirection,
                                 Material.Baked particleMaterial,
                                 int materialFlags,
                                 boolean useAmbientOcclusion) {
            this.all = all;
            this.byDirection = byDirection;
            this.particleMaterial = particleMaterial;
            this.materialFlags = materialFlags;
            this.useAmbientOcclusion = useAmbientOcclusion;
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            if (direction == null) {
                return all;
            }
            return byDirection.getOrDefault(direction, Collections.emptyList());
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return useAmbientOcclusion;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particleMaterial;
        }

        @Override
        public int materialFlags() {
            return materialFlags;
        }
    }
}