package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class EnhancedSlabItemBakedModel implements BakedModel {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final Vector3f VERTICAL_SLAB_FROM = new Vector3f(0.0f, 0.0f, 8.0f);
    private static final Vector3f VERTICAL_SLAB_TO = new Vector3f(16.0f, 16.0f, 16.0f);
    private static final Vector3f STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3f STEP_TO = new Vector3f(16.0f, 8.0f, 16.0f);
    private static final Vector3f VERTICAL_STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);
    private static final Vector3f VERTICAL_STEP_TO = new Vector3f(16.0f, 16.0f, 8.0f);
    private static final Map<BakedModel, EnumMap<ModelType, EnhancedSlabItemBakedModel>> WRAPPER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<BakedModel, EnumMap<ModelType, EnhancedSlabItemBakedModel>> PRESERVED_WRAPPER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ItemTransforms VERTICAL_SLAB_TRANSFORMS = createTransforms(
            transform(75.0f, 45.0f, 0.0f, 0.0f, 2.5f, 0.0f, 0.375f),
            transform(75.0f, 45.0f, 0.0f, -1.25f, -0.25f, 1.5f, 0.375f),
            transform(0.0f, -135.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(0.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(103.0f, 0.0f, 0.0f, 0.0f, 8.0f, 1.75f, 1.0f),
            transform(30.0f, -135.0f, 0.0f, 1.75f, -0.75f, 0.0f, 0.625f),
            transform(0.0f, 0.0f, 0.0f, 0.0f, 3.0f, -1.0f, 0.25f),
            transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.5f, 0.5f)
    );
    private static final ItemTransforms STEP_TRANSFORMS = createTransforms(
            transform(75.0f, 45.0f, 0.0f, 0.0f, 2.5f, 0.0f, 0.375f),
            transform(75.0f, 45.0f, 0.0f, -1.25f, -0.25f, 1.5f, 0.375f),
            transform(0.0f, -135.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(0.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(103.0f, 0.0f, 0.0f, 0.0f, 8.0f, 1.75f, 1.0f),
            transform(30.0f, -135.0f, 0.0f, 1.75f, 2.5f, 0.0f, 0.625f),
            transform(0.0f, 0.0f, 0.0f, -0.75f, 3.0f, 0.0f, 0.25f),
            transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f)
    );
    private static final ItemTransforms VERTICAL_STEP_TRANSFORMS = createTransforms(
            transform(75.0f, 45.0f, 0.0f, 0.0f, 2.5f, 0.0f, 0.375f),
            transform(75.0f, 45.0f, 0.0f, -1.25f, -0.25f, 1.5f, 0.375f),
            transform(0.0f, -135.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(0.0f, 45.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.4f),
            transform(103.0f, 0.0f, 0.0f, 0.0f, 8.0f, 1.75f, 1.0f),
            transform(30.0f, -135.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.625f),
            transform(0.0f, 0.0f, 0.0f, -0.75f, 3.0f, 0.0f, 0.25f),
            transform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f)
    );

    private final BakedModel sourceModel;
    private final List<BakedQuad> quads;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;

    private EnhancedSlabItemBakedModel(BakedModel sourceModel, ModelType modelType, boolean preserveSourceGeometry) {
        this.sourceModel = sourceModel;
        this.particleIcon = sourceModel.getParticleIcon();
        this.transforms = transformsFor(modelType);
        this.quads = List.copyOf(preserveSourceGeometry
                ? buildPreservedSourceQuads(sourceModel, modelType)
                : buildQuads(sourceModel, modelType, this.particleIcon));
    }

    public static BakedModel wrap(BakedModel sourceModel, ModelType modelType) {
        return wrap(sourceModel, modelType, false);
    }

    public static BakedModel wrap(BakedModel sourceModel, ModelType modelType, boolean preserveSourceGeometry) {
        if (sourceModel == null || modelType == ModelType.NONE) {
            return sourceModel;
        }

        Map<BakedModel, EnumMap<ModelType, EnhancedSlabItemBakedModel>> cache = preserveSourceGeometry
                ? PRESERVED_WRAPPER_CACHE
                : WRAPPER_CACHE;
        synchronized (cache) {
            EnumMap<ModelType, EnhancedSlabItemBakedModel> byType = cache.computeIfAbsent(sourceModel, unused -> new EnumMap<>(ModelType.class));
            return byType.computeIfAbsent(modelType, type -> new EnhancedSlabItemBakedModel(sourceModel, type, preserveSourceGeometry));
        }
    }

    public static void clearCache() {
        synchronized (WRAPPER_CACHE) {
            WRAPPER_CACHE.clear();
        }
        synchronized (PRESERVED_WRAPPER_CACHE) {
            PRESERVED_WRAPPER_CACHE.clear();
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return side == null ? this.quads : List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.sourceModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.sourceModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.sourceModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.sourceModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleIcon;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
        return this.sourceModel.getOverrides();
    }

    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return List.of(this);
    }

    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
            return List.of(Sheets.translucentItemSheet());
        }

        return this.sourceModel.getRenderTypes(stack, fabulous);
    }

    private static List<BakedQuad> buildPreservedSourceQuads(BakedModel sourceModel, ModelType modelType) {
        if (modelType == ModelType.VERTICAL_SLAB || modelType == ModelType.VERTICAL_STEP) {
            List<BakedQuad> nativeVertical = ThatchModelGeometry.verticalSlabQuads(sourceModel);
            if (nativeVertical != null) {
                if (modelType == ModelType.VERTICAL_SLAB) {
                    return nativeVertical;
                }
                // Blueprint's template occupies the south half. Its east quarter turns
                // into BaB's canonical north-east Vertical Step without mirrored UVs.
                GeometryTransform eastQuarter = GeometryTransform.resizePreservingOverhang(
                        0.5F, 1.0F, 1.0F, 0.5F, 0.0F, 0.0F);
                return nativeVertical.stream()
                        .map(quad -> transformGeometryQuad(quad, eastQuarter))
                        .map(quad -> transformGeometryQuad(quad, GeometryTransform.ROTATE_Y_COUNTERCLOCKWISE))
                        .toList();
            }
        }
        List<BakedQuad> sourceQuads = collectSourceQuads(sourceModel);
        GeometryTransform transform = switch (modelType) {
            case STEP -> GeometryTransform.resizePreservingOverhang(0.5F, 1.0F, 1.0F, 0.5F, 0.0F, 0.0F);
            case VERTICAL_SLAB -> GeometryTransform.resizePreservingOverhang(1.0F, 1.0F, 0.5F, 0.0F, 0.0F, 0.5F);
            case VERTICAL_STEP -> GeometryTransform.resizePreservingOverhang(0.5F, 1.0F, 0.5F, 0.5F, 0.0F, 0.0F);
            case NONE -> GeometryTransform.IDENTITY;
        };

        List<BakedQuad> transformed = new ArrayList<>(sourceQuads.size());
        for (BakedQuad quad : sourceQuads) {
            if (quad != null) {
                transformed.add(transformGeometryQuad(quad, transform));
            }
        }
        return transformed;
    }

    private static List<BakedQuad> collectSourceQuads(BakedModel sourceModel) {
        List<BakedQuad> quads = new ArrayList<>();
        IdentityHashMap<BakedQuad, Boolean> seen = new IdentityHashMap<>();
        addUniqueQuads(quads, seen, sourceModel.getQuads(null, null, seededRandom()));
        for (Direction direction : Direction.values()) {
            addUniqueQuads(quads, seen, sourceModel.getQuads(null, direction, seededRandom()));
        }
        return quads;
    }

    private static void addUniqueQuads(List<BakedQuad> target, IdentityHashMap<BakedQuad, Boolean> seen, List<BakedQuad> source) {
        if (source == null) {
            return;
        }
        for (BakedQuad quad : source) {
            if (quad != null && seen.put(quad, Boolean.TRUE) == null) {
                target.add(quad);
            }
        }
    }

    static BakedQuad transformGeometryQuad(BakedQuad quad, GeometryTransform transform) {
        if (quad == null || transform == null || transform == GeometryTransform.IDENTITY) {
            return quad;
        }

        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        if (stride < 3) {
            return quad;
        }

        float[][] originalPositions = new float[4][3];
        float[][] movedPositions = new float[4][3];
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float x = Float.intBitsToFloat(vertices[index]);
            float y = Float.intBitsToFloat(vertices[index + 1]);
            float z = Float.intBitsToFloat(vertices[index + 2]);
            originalPositions[vertex] = new float[]{x, y, z};
            float[] moved = transform.apply(x, y, z);
            movedPositions[vertex] = moved;
            vertices[index] = Float.floatToRawIntBits(moved[0]);
            vertices[index + 1] = Float.floatToRawIntBits(moved[1]);
            vertices[index + 2] = Float.floatToRawIntBits(moved[2]);
        }
        if (transform.preserveUvDensity() && stride >= 6) {
            adjustUvDensity(vertices, stride, quad.getDirection(), quad.getSprite(), originalPositions, movedPositions);
        }

        float[] expectedNormal = transformedDirectionVector(quad.getDirection(), transform);
        float[] geometricNormal = faceNormal(vertices, stride);
        if (dot(geometricNormal, expectedNormal) < 0.0F) {
            reverseWinding(vertices, stride);
            geometricNormal = faceNormal(vertices, stride);
        }
        if (lengthSquared(geometricNormal) < 0.000001F) {
            geometricNormal = expectedNormal;
        }
        normalize(geometricNormal);
        if (stride >= 8) {
            int packedNormal = packNormal(geometricNormal[0], geometricNormal[1], geometricNormal[2]);
            for (int vertex = 0; vertex < 4; vertex++) {
                vertices[vertex * stride + 7] = packedNormal;
            }
        }

        Direction direction = closestDirection(expectedNormal, quad.getDirection());
        return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
    }

    private static void adjustUvDensity(int[] vertices, int stride, Direction direction, TextureAtlasSprite sprite,
                                        float[][] originalPositions, float[][] movedPositions) {
        // Undo FaceBakery's inset before cropping, then inset the smaller UV rectangle.
        // Cropping already-inset coordinates shifts the sampling region at cut edges.
        float inset = sprite.uvShrinkRatio();
        rescaleUvAroundCenter(vertices, stride, 1.0F / (1.0F - inset));
        int[] uAxes;
        int[] vAxes;
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            uAxes = new int[]{0};
            vAxes = new int[]{1};
        } else if (direction == Direction.EAST || direction == Direction.WEST) {
            uAxes = new int[]{2};
            vAxes = new int[]{1};
        } else {
            uAxes = new int[]{0, 2};
            vAxes = new int[]{0, 2};
        }
        adjustUvComponent(vertices, stride, 4, uAxes, originalPositions, movedPositions);
        adjustUvComponent(vertices, stride, 5, vAxes, originalPositions, movedPositions);
        rescaleUvAroundCenter(vertices, stride, 1.0F - inset);
    }

    private static void rescaleUvAroundCenter(int[] vertices, int stride, float scale) {
        for (int component = 4; component <= 5; component++) {
            float center = 0.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                center += Float.intBitsToFloat(vertices[vertex * stride + component]) * 0.25F;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                int index = vertex * stride + component;
                float value = Float.intBitsToFloat(vertices[index]);
                vertices[index] = Float.floatToRawIntBits(center + (value - center) * scale);
            }
        }
    }

    private static void adjustUvComponent(int[] vertices, int stride, int component, int[] candidateAxes,
                                          float[][] originalPositions, float[][] movedPositions) {
        float[] values = new float[4];
        float valueMean = 0.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            values[vertex] = Float.intBitsToFloat(vertices[vertex * stride + component]);
            valueMean += values[vertex];
        }
        valueMean *= 0.25F;

        int bestAxis = -1;
        float bestScore = 0.0F;
        float bestSlope = 0.0F;
        float bestIntercept = 0.0F;
        for (int axis : candidateAxes) {
            float coordinateMean = 0.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                coordinateMean += originalPositions[vertex][axis];
            }
            coordinateMean *= 0.25F;

            float covariance = 0.0F;
            float coordinateVariance = 0.0F;
            float valueVariance = 0.0F;
            for (int vertex = 0; vertex < 4; vertex++) {
                float coordinateDelta = originalPositions[vertex][axis] - coordinateMean;
                float valueDelta = values[vertex] - valueMean;
                covariance += coordinateDelta * valueDelta;
                coordinateVariance += coordinateDelta * coordinateDelta;
                valueVariance += valueDelta * valueDelta;
            }
            if (coordinateVariance < 1.0E-8F || valueVariance < 1.0E-12F) {
                continue;
            }
            float score = covariance * covariance / (coordinateVariance * valueVariance);
            if (score > bestScore) {
                bestScore = score;
                bestAxis = axis;
                bestSlope = covariance / coordinateVariance;
                bestIntercept = valueMean - bestSlope * coordinateMean;
            }
        }
        if (bestAxis < 0 || bestScore < 0.999F) {
            return;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            float adjusted = bestSlope * movedPositions[vertex][bestAxis] + bestIntercept;
            vertices[vertex * stride + component] = Float.floatToRawIntBits(adjusted);
        }
    }

    private static float[] transformedDirectionVector(Direction direction, GeometryTransform transform) {
        if (direction == null) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        float[] center = transform.apply(0.5F, 0.5F, 0.5F);
        float[] moved = transform.apply(
                0.5F + direction.getStepX() * 0.25F,
                0.5F + direction.getStepY() * 0.25F,
                0.5F + direction.getStepZ() * 0.25F
        );
        float[] result = new float[]{moved[0] - center[0], moved[1] - center[1], moved[2] - center[2]};
        normalize(result);
        return result;
    }

    private static float[] faceNormal(int[] vertices, int stride) {
        float x0 = Float.intBitsToFloat(vertices[0]);
        float y0 = Float.intBitsToFloat(vertices[1]);
        float z0 = Float.intBitsToFloat(vertices[2]);
        float x1 = Float.intBitsToFloat(vertices[stride]);
        float y1 = Float.intBitsToFloat(vertices[stride + 1]);
        float z1 = Float.intBitsToFloat(vertices[stride + 2]);
        float x2 = Float.intBitsToFloat(vertices[stride * 2]);
        float y2 = Float.intBitsToFloat(vertices[stride * 2 + 1]);
        float z2 = Float.intBitsToFloat(vertices[stride * 2 + 2]);
        float ax = x1 - x0;
        float ay = y1 - y0;
        float az = z1 - z0;
        float bx = x2 - x0;
        float by = y2 - y0;
        float bz = z2 - z0;
        return new float[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float lengthSquared(float[] vector) {
        return dot(vector, vector);
    }

    private static void normalize(float[] vector) {
        float length = (float) Math.sqrt(lengthSquared(vector));
        if (length > 0.000001F) {
            vector[0] /= length;
            vector[1] /= length;
            vector[2] /= length;
        }
    }

    private static int packNormal(float x, float y, float z) {
        int packedX = ((byte) Math.round(Math.max(-1.0F, Math.min(1.0F, x)) * 127.0F)) & 0xFF;
        int packedY = ((byte) Math.round(Math.max(-1.0F, Math.min(1.0F, y)) * 127.0F)) & 0xFF;
        int packedZ = ((byte) Math.round(Math.max(-1.0F, Math.min(1.0F, z)) * 127.0F)) & 0xFF;
        return packedX | (packedY << 8) | (packedZ << 16);
    }

    private static Direction closestDirection(float[] normal, Direction fallback) {
        if (lengthSquared(normal) < 0.000001F) {
            return fallback == null ? Direction.UP : fallback;
        }
        float ax = Math.abs(normal[0]);
        float ay = Math.abs(normal[1]);
        float az = Math.abs(normal[2]);
        if (ay >= ax && ay >= az) {
            return normal[1] >= 0.0F ? Direction.UP : Direction.DOWN;
        }
        if (az >= ax) {
            return normal[2] >= 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
        return normal[0] >= 0.0F ? Direction.EAST : Direction.WEST;
    }

    private static void reverseWinding(int[] vertices, int stride) {
        int first = stride;
        int second = stride * 3;
        for (int i = 0; i < stride; i++) {
            int value = vertices[first + i];
            vertices[first + i] = vertices[second + i];
            vertices[second + i] = value;
        }
    }

    private static List<BakedQuad> buildQuads(BakedModel sourceModel, ModelType modelType, TextureAtlasSprite particleIcon) {
        FaceGeometry faceGeometry = extractFaceData(sourceModel, particleIcon);
        if (faceGeometry.faceData().isEmpty() || faceGeometry.fallback() == null) {
            return sourceModel.getQuads(null, null, seededRandom());
        }

        List<BakedQuad> quads = new ArrayList<>(6);
        switch (modelType) {
            case STEP -> addStepQuads(quads, faceGeometry);
            case VERTICAL_STEP -> addVerticalStepQuads(quads, faceGeometry);
            case VERTICAL_SLAB -> addVerticalSlabQuads(quads, faceGeometry);
            case NONE -> {
                return sourceModel.getQuads(null, null, seededRandom());
            }
        }
        return quads;
    }

    private static void addVerticalSlabQuads(List<BakedQuad> quads, FaceGeometry faceGeometry) {
        Vector3f from = adjustedFrom(VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO, faceGeometry.bounds());
        Vector3f to = adjustedTo(VERTICAL_SLAB_FROM, VERTICAL_SLAB_TO, faceGeometry.bounds());
        addFaceQuads(quads, Direction.NORTH, 0.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.SOUTH, 0.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.WEST, 0.0f, 0.0f, 8.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.EAST, 8.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.UP, 0.0f, shiftedTopFaceV0(8.0f, 16.0f, from, to), 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.DOWN, 0.0f, 0.0f, 16.0f, 8.0f, faceGeometry, from, to);
    }

    private static void addStepQuads(List<BakedQuad> quads, FaceGeometry faceGeometry) {
        Vector3f from = adjustedFrom(STEP_FROM, STEP_TO, faceGeometry.bounds());
        Vector3f to = adjustedTo(STEP_FROM, STEP_TO, faceGeometry.bounds());
        addFaceQuads(quads, Direction.NORTH, 0.0f, 8.0f, 8.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.SOUTH, 8.0f, 8.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.WEST, 0.0f, 8.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.EAST, 0.0f, 8.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.UP, 8.0f, shiftedTopFaceV0(0.0f, 8.0f, from, to), 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.DOWN, 8.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
    }

    private static void addVerticalStepQuads(List<BakedQuad> quads, FaceGeometry faceGeometry) {
        Vector3f from = adjustedFrom(VERTICAL_STEP_FROM, VERTICAL_STEP_TO, faceGeometry.bounds());
        Vector3f to = adjustedTo(VERTICAL_STEP_FROM, VERTICAL_STEP_TO, faceGeometry.bounds());
        addFaceQuads(quads, Direction.NORTH, 8.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.SOUTH, 8.0f, 0.0f, 16.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.WEST, 0.0f, 0.0f, 8.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.EAST, 0.0f, 0.0f, 8.0f, 16.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.UP, 8.0f, shiftedTopFaceV0(0.0f, 16.0f, from, to), 16.0f, 8.0f, faceGeometry, from, to);
        addFaceQuads(quads, Direction.DOWN, 8.0f, 0.0f, 16.0f, 8.0f, faceGeometry, from, to);
    }

    private static void addFaceQuads(List<BakedQuad> quads, Direction direction, float u0, float v0, float u1, float v1,
                                     FaceGeometry faceGeometry, Vector3f from, Vector3f to) {
        List<FaceData> templates = faceGeometry.faceData().get(direction);
        if (templates == null || templates.isEmpty()) {
            templates = Collections.singletonList(faceGeometry.fallback());
        }

        for (FaceData template : templates) {
            BakedQuad quad = bakeFaceQuad(direction, u0, v0, u1, v1, template, from, to);
            if (quad != null) {
                quads.add(quad);
            }
        }
    }

    private static FaceGeometry extractFaceData(BakedModel sourceModel, TextureAtlasSprite particleIcon) {
        EnumMap<Direction, List<FaceData>> faceData = new EnumMap<>(Direction.class);
        IdentityHashMap<BakedQuad, Boolean> seenQuads = new IdentityHashMap<>();
        SourceBounds bounds = new SourceBounds();
        List<BakedQuad> unculled = sourceModel.getQuads(null, null, seededRandom());
        FaceData fallback = addFaceData(faceData, unculled, null, null);
        addBounds(bounds, seenQuads, unculled);

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = sourceModel.getQuads(null, direction, seededRandom());
            fallback = addFaceData(faceData, quads, direction, fallback);
            addBounds(bounds, seenQuads, quads);
        }

        if (fallback == null && particleIcon != null) {
            fallback = new FaceData(particleIcon, -1, true, null);
        }
        if (fallback == null) {
            return new FaceGeometry(faceData, null, bounds);
        }

        for (Direction direction : Direction.values()) {
            faceData.computeIfAbsent(direction, unused -> new ArrayList<>());
        }
        return new FaceGeometry(faceData, fallback, bounds);
    }

    private static FaceData addFaceData(EnumMap<Direction, List<FaceData>> faceData, List<BakedQuad> quads, Direction requestedDirection, FaceData fallback) {
        if (quads == null) {
            return fallback;
        }

        FaceData resolvedFallback = fallback;
        for (BakedQuad quad : quads) {
            if (quad == null || quad.getSprite() == null) {
                continue;
            }

            FaceData data = new FaceData(quad.getSprite(), quad.getTintIndex(), quad.isShade(), extractFaceUv(quad));
            if (resolvedFallback == null) {
                resolvedFallback = data;
            }

            Direction direction = requestedDirection != null ? requestedDirection : classifyQuadDirection(quad);
            if (direction != null) {
                faceData.computeIfAbsent(direction, unused -> new ArrayList<>()).add(data);
            }
        }
        return resolvedFallback;
    }

    private static Direction classifyQuadDirection(BakedQuad quad) {
        if (quad == null) {
            return null;
        }

        int[] vertices = quad.getVertices();
        if (vertices == null || vertices.length < 12) {
            return quad.getDirection();
        }

        int stride = vertices.length / 4;
        float x0 = Float.intBitsToFloat(vertices[0]);
        float y0 = Float.intBitsToFloat(vertices[1]);
        float z0 = Float.intBitsToFloat(vertices[2]);
        float x1 = Float.intBitsToFloat(vertices[stride]);
        float y1 = Float.intBitsToFloat(vertices[stride + 1]);
        float z1 = Float.intBitsToFloat(vertices[stride + 2]);
        float x2 = Float.intBitsToFloat(vertices[stride * 2]);
        float y2 = Float.intBitsToFloat(vertices[stride * 2 + 1]);
        float z2 = Float.intBitsToFloat(vertices[stride * 2 + 2]);

        float edgeAx = x1 - x0;
        float edgeAy = y1 - y0;
        float edgeAz = z1 - z0;
        float edgeBx = x2 - x0;
        float edgeBy = y2 - y0;
        float edgeBz = z2 - z0;

        float normalX = edgeAy * edgeBz - edgeAz * edgeBy;
        float normalY = edgeAz * edgeBx - edgeAx * edgeBz;
        float normalZ = edgeAx * edgeBy - edgeAy * edgeBx;

        float absX = Math.abs(normalX);
        float absY = Math.abs(normalY);
        float absZ = Math.abs(normalZ);
        if (absX < 0.0001F && absY < 0.0001F && absZ < 0.0001F) {
            return quad.getDirection();
        }

        if (absY >= absX && absY >= absZ) {
            return normalY >= 0.0F ? Direction.UP : Direction.DOWN;
        }
        if (absZ >= absX) {
            return normalZ >= 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
        if (absX >= 0.0001F) {
            return normalX >= 0.0F ? Direction.EAST : Direction.WEST;
        }
        return quad.getDirection();
    }

    private static float[] extractFaceUv(BakedQuad quad) {
        if (quad == null || quad.getSprite() == null) {
            return null;
        }

        int[] vertices = quad.getVertices();
        if (vertices == null || vertices.length < 24) {
            return null;
        }

        int stride = vertices.length / 4;
        if (stride < 6) {
            return null;
        }

        TextureAtlasSprite sprite = quad.getSprite();
        float spriteU0 = sprite.getU0();
        float spriteU1 = sprite.getU1();
        float spriteV0 = sprite.getV0();
        float spriteV1 = sprite.getV1();
        float uScale = spriteU1 - spriteU0;
        float vScale = spriteV1 - spriteV0;
        if (Math.abs(uScale) < 0.000001F || Math.abs(vScale) < 0.000001F) {
            return null;
        }

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float atlasU = Float.intBitsToFloat(vertices[index + 4]);
            float atlasV = Float.intBitsToFloat(vertices[index + 5]);
            float localU = 16.0F * (atlasU - spriteU0) / uScale;
            float localV = 16.0F * (atlasV - spriteV0) / vScale;
            minU = Math.min(minU, localU);
            minV = Math.min(minV, localV);
            maxU = Math.max(maxU, localU);
            maxV = Math.max(maxV, localV);
        }

        if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
            return null;
        }
        return new float[]{minU, minV, maxU, maxV};
    }

    private static void addBounds(SourceBounds bounds, IdentityHashMap<BakedQuad, Boolean> seenQuads, List<BakedQuad> quads) {
        if (quads == null) {
            return;
        }

        for (BakedQuad quad : quads) {
            if (quad == null || seenQuads.put(quad, Boolean.TRUE) != null) {
                continue;
            }

            int[] vertices = quad.getVertices();
            if (vertices == null || vertices.length < 12) {
                continue;
            }

            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int index = vertex * stride;
                float x = Float.intBitsToFloat(vertices[index]);
                float y = Float.intBitsToFloat(vertices[index + 1]);
                float z = Float.intBitsToFloat(vertices[index + 2]);
                bounds.include(x * 16.0F, y * 16.0F, z * 16.0F);
            }
        }
    }

    private static Vector3f adjustedFrom(Vector3f from, Vector3f to, SourceBounds bounds) {
        Vector3f adjusted = new Vector3f(from);
        float xSize = to.x() - from.x();
        float zSize = to.z() - from.z();

        float xThickness = bounds.halfWidth();
        if (isHalfBlock(xSize) && shouldAdjustThickness(xThickness)) {
            adjusted.x = alignStart(from.x(), xThickness);
        }
        float zThickness = bounds.halfDepth();
        if (isHalfBlock(zSize) && shouldAdjustThickness(zThickness)) {
            adjusted.z = alignStart(from.z(), zThickness);
        }
        return adjusted;
    }

    private static Vector3f adjustedTo(Vector3f from, Vector3f to, SourceBounds bounds) {
        Vector3f adjusted = new Vector3f(to);
        float xSize = to.x() - from.x();
        float ySize = to.y() - from.y();
        float zSize = to.z() - from.z();

        float yThickness = bounds.height();
        if (isHalfBlock(ySize) && shouldAdjustThickness(yThickness)) {
            adjusted.y = to.y() - (8.0F - yThickness);
        }
        float xThickness = bounds.halfWidth();
        if (isHalfBlock(xSize) && shouldAdjustThickness(xThickness)) {
            adjusted.x = alignEnd(from.x(), xThickness);
        }
        float zThickness = bounds.halfDepth();
        if (isHalfBlock(zSize) && shouldAdjustThickness(zThickness)) {
            adjusted.z = alignEnd(from.z(), zThickness);
        }
        return adjusted;
    }

    private static boolean isHalfBlock(float size) {
        return Math.abs(size - 8.0F) < 0.001F;
    }

    private static boolean shouldAdjustThickness(float thickness) {
        return thickness > 0.0F && Math.abs(thickness - 8.0F) >= 0.001F;
    }

    private static float alignStart(float from, float thickness) {
        return from > 0.0F ? 16.0F - thickness : from;
    }

    private static float alignEnd(float from, float thickness) {
        return from > 0.0F ? 16.0F : from + thickness;
    }

    private static BakedQuad bakeFaceQuad(Direction direction, float u0, float v0, float u1, float v1, FaceData faceData, Vector3f from, Vector3f to) {
        if (faceData == null || faceData.sprite() == null) {
            return null;
        }

        float[] uvValues = mergedFaceUv(direction, u0, v0, u1, v1, faceData.uv(), from, to);
        BlockFaceUV uv = new BlockFaceUV(new float[]{uvValues[0], uvValues[1], uvValues[2], uvValues[3]}, 0);
        BlockElementFace elementFace = new BlockElementFace(null, faceData.tintIndex(), "#texture", uv);
        return FACE_BAKERY.bakeQuad(
                new Vector3f(from),
                new Vector3f(to),
                elementFace,
                faceData.sprite(),
                direction,
                BlockModelRotation.X0_Y0,
                null,
                faceData.shade()
        );
    }

    private static float[] mergedFaceUv(Direction direction, float u0, float v0, float u1, float v1, float[] sourceUv, Vector3f from, Vector3f to) {
        float[] uv = new float[]{u0, v0, u1, v1};
        if (sourceUv == null) {
            if (direction.getAxis().isHorizontal() && shouldShiftUprightSideFaceUv(from, to)) {
                uv[1] = shiftedSideFaceV0(uv[1], uv[3], from, to);
            }
            return uv;
        }

        if (direction.getAxis().isHorizontal()) {
            if (shouldPreserveSourceVerticalCrop(from, to)) {
                uv[1] = sourceUv[1];
                uv[3] = sourceUv[3];
            } else if (shouldShiftUprightSideFaceUv(from, to)) {
                uv[1] = shiftedSideFaceV0(uv[1], uv[3], from, to);
            }
        }
        return uv;
    }

    private static float shiftedTopFaceV0(float v0, float expectedHeight, Vector3f from, Vector3f to) {
        float heightDeficit = expectedHeight - (to.y() - from.y());
        if (heightDeficit > 0.001F) {
            return v0 + heightDeficit;
        }
        return v0;
    }

    private static boolean shouldPreserveSourceVerticalCrop(Vector3f from, Vector3f to) {
        return (to.y() - from.y()) <= 8.001F;
    }

    private static boolean shouldShiftUprightSideFaceUv(Vector3f from, Vector3f to) {
        float actualHeight = to.y() - from.y();
        return actualHeight > 8.001F && actualHeight < 15.999F;
    }

    private static float shiftedSideFaceV0(float v0, float v1, Vector3f from, Vector3f to) {
        float heightDeficit = (v1 - v0) - (to.y() - from.y());
        if (heightDeficit > 0.001F) {
            return v0 + heightDeficit;
        }
        return v0;
    }

    private static RandomSource seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }

    private record FaceGeometry(EnumMap<Direction, List<FaceData>> faceData, FaceData fallback, SourceBounds bounds) {
    }

    private static final class SourceBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void include(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private float height() {
            if (!Float.isFinite(this.minY) || !Float.isFinite(this.maxY)) {
                return 8.0F;
            }
            return Math.max(0.0F, this.maxY - this.minY);
        }

        private float halfWidth() {
            if (!Float.isFinite(this.minX) || !Float.isFinite(this.maxX)) {
                return 8.0F;
            }
            return Math.max(0.0F, (this.maxX - this.minX) * 0.5F);
        }

        private float halfDepth() {
            if (!Float.isFinite(this.minZ) || !Float.isFinite(this.maxZ)) {
                return 8.0F;
            }
            return Math.max(0.0F, (this.maxZ - this.minZ) * 0.5F);
        }
    }

    private static ItemTransforms transformsFor(ModelType modelType) {
        return switch (modelType) {
            case STEP -> STEP_TRANSFORMS;
            case VERTICAL_STEP -> VERTICAL_STEP_TRANSFORMS;
            case VERTICAL_SLAB -> VERTICAL_SLAB_TRANSFORMS;
            case NONE -> ItemTransforms.NO_TRANSFORMS;
        };
    }

    private static ItemTransforms createTransforms(
            ItemTransform thirdPersonLeftHand,
            ItemTransform thirdPersonRightHand,
            ItemTransform firstPersonLeftHand,
            ItemTransform firstPersonRightHand,
            ItemTransform head,
            ItemTransform gui,
            ItemTransform ground,
            ItemTransform fixed
    ) {
        return new ItemTransforms(
                thirdPersonLeftHand,
                thirdPersonRightHand,
                firstPersonLeftHand,
                firstPersonRightHand,
                head,
                gui,
                ground,
                fixed
        );
    }

    private static ItemTransform transform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        return new ItemTransform(
                new Vector3f(rx, ry, rz),
                new Vector3f(tx / 16.0f, ty / 16.0f, tz / 16.0f),
                new Vector3f(scale, scale, scale)
        );
    }

    private record FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, float[] uv) {
    }

    @FunctionalInterface
    interface GeometryTransform {
        GeometryTransform IDENTITY = (x, y, z) -> new float[]{x, y, z};
        GeometryTransform ROTATE_Y_CLOCKWISE = (x, y, z) -> new float[]{1.0F - z, y, x};
        GeometryTransform ROTATE_Y_HALF_TURN = (x, y, z) -> new float[]{1.0F - x, y, 1.0F - z};
        GeometryTransform ROTATE_Y_COUNTERCLOCKWISE = (x, y, z) -> new float[]{z, y, 1.0F - x};

        float[] apply(float x, float y, float z);

        default boolean preserveUvDensity() {
            return false;
        }

        /** Resize the block body without shortening Blueprint's straw beyond its edges. */
        static GeometryTransform resizePreservingOverhang(float scaleX, float scaleY, float scaleZ,
                                                          float offsetX, float offsetY, float offsetZ) {
            return new GeometryTransform() {
                @Override
                public float[] apply(float x, float y, float z) {
                    return new float[]{resizeCoordinate(x, scaleX, offsetX),
                            resizeCoordinate(y, scaleY, offsetY), resizeCoordinate(z, scaleZ, offsetZ)};
                }

                @Override
                public boolean preserveUvDensity() {
                    return true;
                }
            };
        }

        private static float resizeCoordinate(float coordinate, float scale, float offset) {
            float body = Math.max(0.0F, Math.min(1.0F, coordinate));
            return offset + body * scale + (coordinate - body);
        }
    }
}
