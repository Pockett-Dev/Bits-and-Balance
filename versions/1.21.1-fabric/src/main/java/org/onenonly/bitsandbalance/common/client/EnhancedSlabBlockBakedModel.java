package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import org.joml.Vector3f;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class EnhancedSlabBlockBakedModel implements BakedModel, FabricBakedModel {
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    private final BakedModel sourceModel;
    private final BakedModel baseModel;
    private final ModelType modelType;
    private final Map<String, List<BakedQuad>> quadsByState = Collections.synchronizedMap(new HashMap<>());

    private EnhancedSlabBlockBakedModel(BakedModel sourceModel, ModelType modelType) {
        this.sourceModel = sourceModel;
        this.modelType = modelType;
        this.baseModel = EnhancedSlabItemBakedModel.wrap(sourceModel, modelType);
    }

    public static BakedModel wrap(BakedModel sourceModel, ModelType modelType) {
        if (sourceModel == null || modelType == ModelType.NONE) {
            return sourceModel;
        }
        return new EnhancedSlabBlockBakedModel(sourceModel, modelType);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (side != null) {
            return List.of();
        }
        if (state == null) {
            return this.baseModel.getQuads(null, null, random);
        }
        return this.quadsByState.computeIfAbsent(state.toString(), unused -> buildStateQuads(state, random));
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter world,
                               BlockState state,
                               BlockPos pos,
                               Supplier<RandomSource> randomSupplier,
                               RenderContext context) {
        BlockState sourceState = EnhancedSlabFabricRenderUtil.sourceRenderStateForOwner(state);
        RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(sourceState);
        if (sourceState == null || material == null) {
            return;
        }
        for (BakedQuad quad : getQuads(state, null, randomSupplier.get())) {
            EnhancedSlabFabricRenderUtil.emitQuad(context, quad, material, world, pos, sourceState, null);
        }
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
        return this.sourceModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.sourceModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.sourceModel.getOverrides();
    }

    private List<BakedQuad> buildStateQuads(BlockState state, RandomSource random) {
        if (this.modelType == ModelType.VERTICAL_SLAB
                && state.hasProperty(VerticalSlabBlock.DOUBLE)
                && state.getValue(VerticalSlabBlock.DOUBLE)) {
            return buildDoubleVerticalSlabQuads(state, random);
        }

        PointTransform transform = transformFor(state);
        return this.baseModel.getQuads(null, null, random).stream()
                .map(quad -> transformQuad(quad, transform))
                .toList();
    }

    private List<BakedQuad> buildDoubleVerticalSlabQuads(BlockState state, RandomSource random) {
        Direction facing = state.hasProperty(VerticalSlabBlock.FACING)
                ? state.getValue(VerticalSlabBlock.FACING)
                : Direction.SOUTH;
        List<BakedQuad> sourceQuads = this.baseModel.getQuads(null, null, random);
        List<BakedQuad> quads = new ArrayList<>(sourceQuads.size() * 2);
        appendTransformedQuads(quads, sourceQuads, verticalSlabTransform(facing));
        appendTransformedQuads(quads, sourceQuads, verticalSlabTransform(facing.getOpposite()));
        return List.copyOf(quads);
    }

    private static void appendTransformedQuads(List<BakedQuad> target, List<BakedQuad> source, PointTransform transform) {
        for (BakedQuad quad : source) {
            target.add(transform == PointTransform.IDENTITY ? quad : transformQuad(quad, transform));
        }
    }

    private PointTransform transformFor(BlockState state) {
        return switch (this.modelType) {
            case VERTICAL_SLAB -> verticalSlabTransform(state);
            case STEP -> stepTransform(state);
            case VERTICAL_STEP -> verticalStepTransform(state);
            case NONE -> PointTransform.IDENTITY;
        };
    }

    private static PointTransform verticalSlabTransform(BlockState state) {
        Direction facing = state.hasProperty(VerticalSlabBlock.FACING)
                ? state.getValue(VerticalSlabBlock.FACING)
                : Direction.SOUTH;
        return verticalSlabTransform(facing);
    }

    private static PointTransform verticalSlabTransform(Direction facing) {
        return switch (facing) {
            case NORTH -> (x, y, z) -> new float[]{1.0F - x, y, 1.0F - z};
            case EAST -> (x, y, z) -> new float[]{z, y, 1.0F - x};
            case WEST -> (x, y, z) -> new float[]{1.0F - z, y, x};
            default -> PointTransform.IDENTITY;
        };
    }

    private static PointTransform stepTransform(BlockState state) {
        Direction facing = state.hasProperty(StepBlock.FACING) ? state.getValue(StepBlock.FACING) : Direction.EAST;
        boolean top = state.hasProperty(StepBlock.TOP) && state.getValue(StepBlock.TOP);
        PointTransform horizontal = switch (facing) {
            case WEST -> (x, y, z) -> new float[]{1.0F - x, y, z};
            case SOUTH -> (x, y, z) -> new float[]{z, y, x};
            case NORTH -> (x, y, z) -> new float[]{1.0F - z, y, 1.0F - x};
            default -> PointTransform.IDENTITY;
        };
        if (!top) {
            return horizontal;
        }
        return (x, y, z) -> {
            float[] moved = horizontal.apply(x, y, z);
            moved[1] = moved[1] + 0.5F;
            return moved;
        };
    }

    private static PointTransform verticalStepTransform(BlockState state) {
        Direction facing = state.hasProperty(VerticalStepBlock.FACING) ? state.getValue(VerticalStepBlock.FACING) : Direction.NORTH;
        Direction side = state.hasProperty(VerticalStepBlock.SIDE) ? state.getValue(VerticalStepBlock.SIDE) : Direction.EAST;
        boolean flipX = side == Direction.WEST;
        boolean flipZ = facing == Direction.SOUTH;
        if (!flipX && !flipZ) {
            return PointTransform.IDENTITY;
        }
        return (x, y, z) -> new float[]{flipX ? 1.0F - x : x, y, flipZ ? 1.0F - z : z};
    }

    private static BakedQuad transformQuad(BakedQuad quad, PointTransform transform) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float x = Float.intBitsToFloat(vertices[index]);
            float y = Float.intBitsToFloat(vertices[index + 1]);
            float z = Float.intBitsToFloat(vertices[index + 2]);
            float[] moved = transform.apply(x, y, z);
            vertices[index] = Float.floatToRawIntBits(moved[0]);
            vertices[index + 1] = Float.floatToRawIntBits(moved[1]);
            vertices[index + 2] = Float.floatToRawIntBits(moved[2]);
        }
        Direction direction = transform == PointTransform.IDENTITY
                ? quad.getDirection()
                : transformDirection(quad.getDirection(), transform);
        BakedQuad rebaked = rebakeWithBlockFaceUv(vertices, stride, direction, quad);
        if (rebaked != null) {
            return rebaked;
        }
        if (hasOppositeWinding(vertices, stride, direction)) {
            reverseWinding(vertices, stride);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
    }

    private static BakedQuad rebakeWithBlockFaceUv(int[] vertices, int stride, Direction direction, BakedQuad template) {
        if (direction == null || template == null || template.getSprite() == null || stride < 3 || vertices.length < stride * 4) {
            return null;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float x = Float.intBitsToFloat(vertices[index]);
            float y = Float.intBitsToFloat(vertices[index + 1]);
            float z = Float.intBitsToFloat(vertices[index + 2]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
            return null;
        }

        Vector3f from = new Vector3f(minX * 16.0F, minY * 16.0F, minZ * 16.0F);
        Vector3f to = new Vector3f(maxX * 16.0F, maxY * 16.0F, maxZ * 16.0F);
        float[] uv = defaultUv(direction, from, to);
        float[] templateUv = templateUv(template, stride);
        if (templateUv != null && direction.getAxis().isHorizontal() && shouldPreserveTemplateSideUv(from, to)) {
            uv[1] = templateUv[1];
            uv[3] = templateUv[3];
        }
        BlockFaceUV blockFaceUV = new BlockFaceUV(new float[]{uv[0], uv[1], uv[2], uv[3]}, 0);
        BlockElementFace face = new BlockElementFace(null, template.getTintIndex(), "#texture", blockFaceUV);
        BakedQuad rebaked = FACE_BAKERY.bakeQuad(
                from,
                to,
                face,
                template.getSprite(),
                direction,
                BlockModelRotation.X0_Y0,
                null,
                template.isShade()
        );
        if (rebaked == null || !hasOppositeWinding(rebaked.getVertices(), rebaked.getVertices().length / 4, direction)) {
            return rebaked;
        }
        int[] rewound = rebaked.getVertices().clone();
        reverseWinding(rewound, rewound.length / 4);
        return new BakedQuad(rewound, rebaked.getTintIndex(), direction, rebaked.getSprite(), rebaked.isShade());
    }

    private static float[] templateUv(BakedQuad template, int stride) {
        if (template == null || template.getSprite() == null || template.getVertices().length < stride * 4 || stride < 6) {
            return null;
        }

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        int[] templateVertices = template.getVertices();
        for (int vertex = 0; vertex < 4; vertex++) {
            int index = vertex * stride;
            float u = template.getSprite().getUOffset(Float.intBitsToFloat(templateVertices[index + 4])) * 16.0F;
            float v = template.getSprite().getVOffset(Float.intBitsToFloat(templateVertices[index + 5])) * 16.0F;
            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);
        }
        if (!Float.isFinite(minU) || !Float.isFinite(minV) || !Float.isFinite(maxU) || !Float.isFinite(maxV)) {
            return null;
        }
        return new float[]{minU, minV, maxU, maxV};
    }

    private static boolean shouldPreserveTemplateSideUv(Vector3f from, Vector3f to) {
        return to.y() - from.y() <= 8.001F;
    }

    private static float[] defaultUv(Direction direction, Vector3f from, Vector3f to) {
        float fx = from.x();
        float fy = from.y();
        float fz = from.z();
        float tx = to.x();
        float ty = to.y();
        float tz = to.z();
        return switch (direction) {
            case NORTH -> new float[]{16.0F - tx, 16.0F - ty, 16.0F - fx, 16.0F - fy};
            case SOUTH -> new float[]{fx, 16.0F - ty, tx, 16.0F - fy};
            case WEST -> new float[]{fz, 16.0F - ty, tz, 16.0F - fy};
            case EAST -> new float[]{16.0F - tz, 16.0F - ty, 16.0F - fz, 16.0F - fy};
            case UP -> new float[]{fx, fz, tx, tz};
            case DOWN -> new float[]{fx, 16.0F - tz, tx, 16.0F - fz};
        };
    }

    private static boolean hasOppositeWinding(int[] vertices, int stride, Direction direction) {
        if (direction == null || vertices.length < stride * 3 || stride < 3) {
            return false;
        }

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
        float dot = normalX * direction.getStepX()
                + normalY * direction.getStepY()
                + normalZ * direction.getStepZ();
        return dot < -0.000001F;
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

    private static Direction transformDirection(Direction direction, PointTransform transform) {
        float[] center = transform.apply(0.5F, 0.5F, 0.5F);
        float[] moved = transform.apply(
                0.5F + direction.getStepX() * 0.25F,
                0.5F + direction.getStepY() * 0.25F,
                0.5F + direction.getStepZ() * 0.25F
        );
        float dx = moved[0] - center[0];
        float dy = moved[1] - center[1];
        float dz = moved[2] - center[2];
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        float az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return dy >= 0.0F ? Direction.UP : Direction.DOWN;
        }
        if (az >= ax) {
            return dz >= 0.0F ? Direction.SOUTH : Direction.NORTH;
        }
        return dx >= 0.0F ? Direction.EAST : Direction.WEST;
    }

    @FunctionalInterface
    private interface PointTransform {
        PointTransform IDENTITY = (x, y, z) -> new float[]{x, y, z};

        float[] apply(float x, float y, float z);
    }
}
