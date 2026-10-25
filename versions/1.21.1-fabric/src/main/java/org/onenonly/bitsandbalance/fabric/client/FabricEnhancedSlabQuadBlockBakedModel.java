package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabFabricRenderUtil;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

final class FabricEnhancedSlabQuadBlockBakedModel extends ForwardingBakedModel implements FabricBakedModel {
    private static final int ALL_DIRECTIONS = directionMask(Direction.values());

    private final boolean vertical;
    private final Map<BakedModel, BakedModel> partModelCache = new WeakHashMap<>();

    private FabricEnhancedSlabQuadBlockBakedModel(BakedModel delegate, boolean vertical) {
        this.wrapped = delegate;
        this.vertical = vertical;
    }

    static BakedModel wrap(BakedModel delegate, boolean vertical) {
        if (delegate == null) {
            return null;
        }
        return new FabricEnhancedSlabQuadBlockBakedModel(delegate, vertical);
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
        Object slabs = EnhancedSlabTerrainRenderHelper.materialSource(world, pos, state);
        if (slabs == null || !hasAnySlot(slabs)) {
            super.emitBlockQuads(world, state, pos, randomSupplier, context);
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        boolean emitted = false;
        for (int index = 0; index < 4; index++) {
            BlockState slab = slot(slabs, index);
            if (slab == null || slab.isAir()) {
                continue;
            }

            RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(slab);
            if (material == null) {
                continue;
            }

            BakedModel sourceModel = dispatcher.getBlockModel(slab);
            BakedModel partModel = partModel(sourceModel);
            BlockState partState = this.vertical ? verticalPartState(index) : stepPartState(state, index);
            int visibleMask = this.vertical ? verticalVisibleMask(index, slabs) : stepVisibleMask(state, index, slabs);
            for (BakedQuad quad : partModel.getQuads(partState, null, randomSupplier.get())) {
                if (!isVisible(visibleMask, quad.getDirection())) {
                    continue;
                }
                BakedQuad renderQuad = this.vertical ? quad : remapStepQuadSideUv(quad, index, slabs);
                emitted |= EnhancedSlabFabricRenderUtil.emitQuad(context, renderQuad, material, world, pos, slab, null);
            }
        }

        if (!emitted) {
            super.emitBlockQuads(world, state, pos, randomSupplier, context);
        }
    }

    private BakedModel partModel(BakedModel sourceModel) {
        synchronized (this.partModelCache) {
            return this.partModelCache.computeIfAbsent(sourceModel, model ->
                    EnhancedSlabBlockBakedModel.wrap(model, this.vertical ? ModelType.VERTICAL_STEP : ModelType.STEP));
        }
    }

    private static BlockState stepPartState(BlockState quadState, int index) {
        Direction.Axis axis = quadState != null && quadState.hasProperty(QuadStepBlock.AXIS)
                ? quadState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        BlockState state = CommonBlocks.STEP != null ? CommonBlocks.STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(StepBlock.FACING)) {
            state = state.setValue(StepBlock.FACING, stepFacing(axis, index));
        }
        if (state.hasProperty(StepBlock.TOP)) {
            state = state.setValue(StepBlock.TOP, index >= 2);
        }
        return state;
    }

    private static BlockState verticalPartState(int index) {
        BlockState state = CommonBlocks.VERTICAL_STEP != null ? CommonBlocks.VERTICAL_STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(VerticalStepBlock.FACING)) {
            state = state.setValue(VerticalStepBlock.FACING, index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH);
        }
        if (state.hasProperty(VerticalStepBlock.SIDE)) {
            state = state.setValue(VerticalStepBlock.SIDE, index == 1 || index == 3 ? Direction.EAST : Direction.WEST);
        }
        return state;
    }

    private static Direction stepFacing(Direction.Axis axis, int index) {
        boolean positive = index == 1 || index == 3;
        return axis == Direction.Axis.Z
                ? (positive ? Direction.SOUTH : Direction.NORTH)
                : (positive ? Direction.EAST : Direction.WEST);
    }

    private static int stepVisibleMask(BlockState quadState, int index, Object slabs) {
        Direction.Axis axis = quadState != null && quadState.hasProperty(QuadStepBlock.AXIS)
                ? quadState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        int mask = ALL_DIRECTIONS;
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, verticalNeighbor))) {
            mask = without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int verticalVisibleMask(int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, eastWestNeighbor))) {
            mask = without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, northSouthNeighbor))) {
            mask = without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static boolean hasAnySlot(Object slabs) {
        for (int index = 0; index < 4; index++) {
            BlockState slab = slot(slabs, index);
            if (slab != null && !slab.isAir()) {
                return true;
            }
        }
        return false;
    }

    private static BlockState prepareSourceState(BlockState state) {
        return EnhancedSlabFabricRenderUtil.normalizeSlabState(state);
    }

    private static BlockState slot(Object slabs, int index) {
        return prepareSourceState(EnhancedSlabTerrainRenderHelper.materialSlot(slabs, index));
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }

    private static BakedQuad remapStepQuadSideUv(BakedQuad quad, int index, Object slabs) {
        Direction direction = quad == null ? null : quad.getDirection();
        if (quad == null || direction == null || !direction.getAxis().isHorizontal()) {
            return quad;
        }
        int verticalNeighbor = index >= 2 ? index - 2 : index + 2;
        if (!sameRenderMaterial(slot(slabs, index), slot(slabs, verticalNeighbor))) {
            return quad;
        }
        return remapSideQuadToFullBlockUv(quad, index);
    }

    private static BakedQuad remapSideQuadToFullBlockUv(BakedQuad quad, int index) {
        TextureAtlasSprite sprite = quad.getSprite();
        Direction direction = quad.getDirection();
        int[] vertices = quad.getVertices();
        if (sprite == null || direction == null || vertices == null || vertices.length < 24) {
            return quad;
        }

        int stride = vertices.length / 4;
        if (stride < 6) {
            return quad;
        }

        int[] remapped = vertices.clone();
        if (index < 2) {
            extendBottomStepSideToJoin(remapped, stride);
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(remapped[offset]);
            float y = Float.intBitsToFloat(remapped[offset + 1]);
            float z = Float.intBitsToFloat(remapped[offset + 2]);
            float u = fullBlockSideU(direction, x, z);
            float v = clamp16(16.0F - y * 16.0F);
            remapped[offset + 4] = Float.floatToRawIntBits(sprite.getU(u / 16.0F));
            remapped[offset + 5] = Float.floatToRawIntBits(sprite.getV(v / 16.0F));
        }
        return new BakedQuad(remapped, quad.getTintIndex(), direction, sprite, quad.isShade());
    }

    private static void extendBottomStepSideToJoin(int[] vertices, int stride) {
        float maxY = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float y = Float.intBitsToFloat(vertices[vertex * stride + 1]);
            maxY = Math.max(maxY, y);
        }
        if (maxY >= 0.499F) {
            return;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            int yOffset = vertex * stride + 1;
            float y = Float.intBitsToFloat(vertices[yOffset]);
            if (Math.abs(y - maxY) < 0.0001F) {
                vertices[yOffset] = Float.floatToRawIntBits(0.5F);
            }
        }
    }

    private static float fullBlockSideU(Direction direction, float x, float z) {
        return clamp16(switch (direction) {
            case NORTH -> 16.0F - x * 16.0F;
            case SOUTH -> x * 16.0F;
            case WEST -> z * 16.0F;
            case EAST -> 16.0F - z * 16.0F;
            default -> 0.0F;
        });
    }

    private static float clamp16(float value) {
        return Math.max(0.0F, Math.min(16.0F, value));
    }

    private static boolean isVisible(int mask, Direction direction) {
        return direction == null || (mask & (1 << direction.ordinal())) != 0;
    }

    private static int without(int mask, Direction direction) {
        return direction == null ? mask : mask & ~(1 << direction.ordinal());
    }

    private static int directionMask(Direction... directions) {
        int mask = 0;
        for (Direction direction : directions) {
            if (direction != null) {
                mask |= 1 << direction.ordinal();
            }
        }
        return mask;
    }

}
