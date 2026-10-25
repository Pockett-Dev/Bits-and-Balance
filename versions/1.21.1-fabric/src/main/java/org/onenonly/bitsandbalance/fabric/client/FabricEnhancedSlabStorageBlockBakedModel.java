package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabFabricRenderUtil;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

final class FabricEnhancedSlabStorageBlockBakedModel extends ForwardingBakedModel implements FabricBakedModel {
    private static final int ALL_DIRECTIONS = directionMask(Direction.values());

    private final ModelType modelType;
    private final Map<BakedModel, BakedModel> partModelCache = new WeakHashMap<>();

    private FabricEnhancedSlabStorageBlockBakedModel(BakedModel delegate, ModelType modelType) {
        this.wrapped = delegate;
        this.modelType = modelType;
    }

    static BakedModel wrap(BakedModel delegate, ModelType modelType) {
        if (delegate == null || modelType == ModelType.NONE) {
            return delegate;
        }
        return new FabricEnhancedSlabStorageBlockBakedModel(delegate, modelType);
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

        boolean emitted = switch (this.modelType) {
            case VERTICAL_SLAB -> emitVerticalSlab(world, state, pos, slabs, randomSupplier, context);
            case STEP, VERTICAL_STEP, NONE -> false;
        };
        if (!emitted) {
            super.emitBlockQuads(world, state, pos, randomSupplier, context);
        }
    }

    private boolean emitVerticalSlab(BlockAndTintGetter world,
                                     BlockState state,
                                     BlockPos pos,
                                     Object slabs,
                                     Supplier<RandomSource> randomSupplier,
                                     RenderContext context) {
        Direction facing = state != null && state.hasProperty(VerticalSlabBlock.FACING)
                ? state.getValue(VerticalSlabBlock.FACING)
                : Direction.NORTH;
        boolean isDouble = state != null && state.hasProperty(VerticalSlabBlock.DOUBLE)
                && state.getValue(VerticalSlabBlock.DOUBLE);

        BlockState facingSlab = EnhancedSlabFabricRenderUtil.normalizeSlabState(slot(slabs, 0));
        BlockState oppositeSlab = EnhancedSlabFabricRenderUtil.normalizeSlabState(slot(slabs, 1));
        boolean emitted = false;
        if (facingSlab != null && !facingSlab.isAir()) {
            int visibleMask = isDouble && oppositeSlab != null && !oppositeSlab.isAir()
                    ? without(ALL_DIRECTIONS, facing.getOpposite())
                    : ALL_DIRECTIONS;
            emitted |= emitPart(world, pos, facingSlab, verticalPartState(facing), visibleMask, randomSupplier, context);
        }
        if (isDouble && oppositeSlab != null && !oppositeSlab.isAir()) {
            emitted |= emitPart(world, pos, oppositeSlab, verticalPartState(facing.getOpposite()),
                    without(ALL_DIRECTIONS, facing), randomSupplier, context);
        }
        return emitted;
    }

    private boolean emitPart(BlockAndTintGetter world,
                             BlockPos pos,
                             BlockState slab,
                             BlockState partState,
                             int visibleMask,
                             Supplier<RandomSource> randomSupplier,
                             RenderContext context) {
        RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(slab);
        if (material == null) {
            return false;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel sourceModel = dispatcher.getBlockModel(slab);
        BakedModel partModel = partModel(sourceModel);
        if (partModel == null) {
            return false;
        }

        boolean emitted = false;
        for (BakedQuad quad : partModel.getQuads(partState, null, randomSupplier.get())) {
            if (!isVisible(visibleMask, quad.getDirection())) {
                continue;
            }
            emitted |= EnhancedSlabFabricRenderUtil.emitQuad(context, quad, material, world, pos, slab, null);
        }
        return emitted;
    }

    private BakedModel partModel(BakedModel sourceModel) {
        if (sourceModel == null) {
            return null;
        }
        synchronized (this.partModelCache) {
            return this.partModelCache.computeIfAbsent(sourceModel, model ->
                    EnhancedSlabBlockBakedModel.wrap(model, ModelType.VERTICAL_SLAB));
        }
    }

    private static BlockState verticalPartState(Direction facing) {
        BlockState state = CommonBlocks.VERTICAL_SLAB != null ? CommonBlocks.VERTICAL_SLAB.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(VerticalSlabBlock.FACING)) {
            state = state.setValue(VerticalSlabBlock.FACING, facing);
        }
        if (state.hasProperty(VerticalSlabBlock.DOUBLE)) {
            state = state.setValue(VerticalSlabBlock.DOUBLE, false);
        }
        if (state.hasProperty(VerticalSlabBlock.WATERLOGGED)) {
            state = state.setValue(VerticalSlabBlock.WATERLOGGED, false);
        }
        return state;
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

    private static BlockState slot(Object slabs, int index) {
        return EnhancedSlabTerrainRenderHelper.materialSlot(slabs, index);
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
