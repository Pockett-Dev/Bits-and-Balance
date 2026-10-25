package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabFabricRenderUtil;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport.ModelType;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class NeoForgeSodiumEnhancedSlabStorageBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel, FabricBakedModel {
    private static final int ALL_DIRECTIONS = directionMask(Direction.values());

    private final NeoForgeEnhancedSlabStorageBakedModel delegate;
    private final NeoForgeEnhancedSlabStorageBakedModel.Kind kind;
    private final Map<BakedModel, EnumMap<ModelType, BakedModel>> partModelCache = Collections.synchronizedMap(new WeakHashMap<>());

    private NeoForgeSodiumEnhancedSlabStorageBakedModel(BakedModel originalModel, NeoForgeEnhancedSlabStorageBakedModel.Kind kind) {
        super(originalModel);
        this.delegate = (NeoForgeEnhancedSlabStorageBakedModel) NeoForgeEnhancedSlabStorageBakedModel.wrap(originalModel, kind);
        this.kind = kind;
    }

    public static BakedModel wrap(BakedModel originalModel, NeoForgeEnhancedSlabStorageBakedModel.Kind kind) {
        return originalModel == null || kind == null ? originalModel : new NeoForgeSodiumEnhancedSlabStorageBakedModel(originalModel, kind);
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
        if (!Config.enableEnhancedSlabs || world == null || pos == null || context == null) {
            emitFallback(world, state, pos, randomSupplier, context);
            return;
        }

        NeoForgeEnhancedSlabStorageBakedModel.RenderData data = this.delegate.collectRenderData(world, pos, state);
        if (data == null || !hasAnySlot(data.slots())) {
            emitFallback(world, state, pos, randomSupplier, context);
            return;
        }

        BlockState ownerState = data.state() == null ? state : data.state();
        NeoForgeEnhancedSlabStorageBakedModel.Kind renderKind = data.kind() == null ? this.kind : data.kind();
        boolean emitted = switch (renderKind) {
            case MIXED_SLAB -> emitMixedSlab(world, pos, data.slots(), randomSupplier, context);
            case VERTICAL_SLAB -> emitVerticalSlab(world, ownerState, pos, data.slots(), randomSupplier, context);
            case STEP -> emitStep(world, ownerState, pos, slot(data.slots(), 0), randomSupplier, context);
            case QUAD_STEP -> emitQuadStep(world, ownerState, pos, data.slots(), randomSupplier, context);
            case VERTICAL_STEP -> emitVerticalStep(world, ownerState, pos, slot(data.slots(), 0), randomSupplier, context);
            case QUAD_VERTICAL_STEP -> emitQuadVerticalStep(world, pos, data.slots(), randomSupplier, context);
        };

        if (!emitted) {
            emitFallback(world, state, pos, randomSupplier, context);
        }
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return this.delegate.getQuads(state, side, random);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData modelData, RenderType renderType) {
        return this.delegate.getQuads(state, side, random, modelData, renderType);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return this.delegate.getModelData(level, pos, state, modelData);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        return this.delegate.getRenderTypes(state, random, modelData);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        return this.delegate.getParticleIcon(modelData);
    }

    private boolean emitMixedSlab(BlockAndTintGetter world,
                                  BlockPos pos,
                                  BlockState[] slots,
                                  Supplier<RandomSource> randomSupplier,
                                  RenderContext context) {
        BlockRenderDispatcher dispatcher = dispatcher();
        if (dispatcher == null) {
            return false;
        }

        BlockState bottom = slot(slots, 0);
        BlockState top = slot(slots, 1);
        BlockState fullThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(bottom, top);
        if (fullThatch != null) {
            return emitLegacyModel(world, pos, fullThatch,
                    ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(fullThatch), fullThatch),
                    randomSupplier, context);
        }
        boolean emitted = false;
        if (bottom != null) {
            emitted |= emitLegacyModel(world, pos, bottom,
                    LegacyEnhancedSlabRenderHelper.createHorizontalSlabModel(dispatcher, bottom, SlabType.BOTTOM, top != null ? Direction.UP : null),
                    randomSupplier, context);
        }
        if (top != null) {
            emitted |= emitLegacyModel(world, pos, top,
                    LegacyEnhancedSlabRenderHelper.createHorizontalSlabModel(dispatcher, top, SlabType.TOP, bottom != null ? Direction.DOWN : null),
                    randomSupplier, context);
        }
        return emitted;
    }

    private boolean emitVerticalSlab(BlockAndTintGetter world,
                                     BlockState state,
                                     BlockPos pos,
                                     BlockState[] slots,
                                     Supplier<RandomSource> randomSupplier,
                                     RenderContext context) {
        Direction facing = state != null && state.hasProperty(VerticalSlabBlock.FACING)
                ? state.getValue(VerticalSlabBlock.FACING)
                : Direction.NORTH;
        boolean doubled = state != null && state.hasProperty(VerticalSlabBlock.DOUBLE) && state.getValue(VerticalSlabBlock.DOUBLE);
        BlockState facingSlab = slot(slots, 0);
        BlockState oppositeSlab = slot(slots, 1);
        BlockState completeThatch = doubled
                ? SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab)
                : null;
        if (completeThatch != null) {
            BlockRenderDispatcher dispatcher = dispatcher();
            return dispatcher != null && emitLegacyModel(world, pos, completeThatch,
                    ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch),
                    randomSupplier, context);
        }
        boolean emitted = false;
        if (facingSlab != null) {
            int visibleMask = doubled && oppositeSlab != null ? without(ALL_DIRECTIONS, facing.getOpposite()) : ALL_DIRECTIONS;
            emitted |= emitPart(world, pos, facingSlab, verticalSlabPartState(facing), ModelType.VERTICAL_SLAB, visibleMask, randomSupplier, context);
        }
        if (doubled && oppositeSlab != null) {
            int visibleMask = facingSlab != null ? without(ALL_DIRECTIONS, facing) : ALL_DIRECTIONS;
            emitted |= emitPart(world, pos, oppositeSlab, verticalSlabPartState(facing.getOpposite()), ModelType.VERTICAL_SLAB, visibleMask, randomSupplier, context);
        }
        return emitted;
    }

    private boolean emitStep(BlockAndTintGetter world,
                             BlockState state,
                             BlockPos pos,
                             BlockState slab,
                             Supplier<RandomSource> randomSupplier,
                             RenderContext context) {
        if (slab == null) {
            return false;
        }
        return emitPart(world, pos, slab, stepPartState(state), ModelType.STEP, ALL_DIRECTIONS, randomSupplier, context);
    }

    private boolean emitQuadStep(BlockAndTintGetter world,
                                 BlockState state,
                                 BlockPos pos,
                                 BlockState[] slots,
                                 Supplier<RandomSource> randomSupplier,
                                 RenderContext context) {
        BlockState completeThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(slots);
        if (completeThatch != null) {
            BlockRenderDispatcher dispatcher = dispatcher();
            return dispatcher != null && emitLegacyModel(world, pos, completeThatch,
                    ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch), randomSupplier, context);
        }
        BlockState[] stackedThatch = new BlockState[2];
        boolean emitted = false;
        for (int bottomIndex = 0; bottomIndex < 2; bottomIndex++) {
            BlockState fullState = ThatchHalfBlockBakedModel.fullStateForStackedPair(slots, bottomIndex);
            if (fullState != null) {
                stackedThatch[bottomIndex] = fullState;
                BlockRenderDispatcher dispatcher = dispatcher();
                if (dispatcher == null) {
                    return false;
                }
                Direction.Axis axis = state != null && state.hasProperty(QuadStepBlock.AXIS)
                        ? state.getValue(QuadStepBlock.AXIS) : Direction.Axis.X;
                BakedModel model = ThatchHalfBlockBakedModel.wrap(
                        dispatcher.getBlockModel(fullState), fullState, stepFacing(axis, bottomIndex));
                emitted |= emitLegacyModel(world, pos, fullState, model, randomSupplier, context);
            }
        }
        for (int index = 0; index < Math.min(4, slots.length); index++) {
            if (stackedThatch[index & 1] != null) {
                continue;
            }
            BlockState slab = slot(slots, index);
            if (slab == null) {
                continue;
            }
            emitted |= emitPart(world, pos, slab, stepPartState(state, index), ModelType.STEP, stepVisibleMask(state, index, slots), randomSupplier, context);
        }
        return emitted;
    }

    private boolean emitVerticalStep(BlockAndTintGetter world,
                                     BlockState state,
                                     BlockPos pos,
                                     BlockState slab,
                                     Supplier<RandomSource> randomSupplier,
                                     RenderContext context) {
        if (slab == null) {
            return false;
        }
        return emitPart(world, pos, slab, verticalStepPartState(state), ModelType.VERTICAL_STEP, ALL_DIRECTIONS, randomSupplier, context);
    }

    private boolean emitQuadVerticalStep(BlockAndTintGetter world,
                                         BlockPos pos,
                                         BlockState[] slots,
                                         Supplier<RandomSource> randomSupplier,
                                         RenderContext context) {
        BlockState completeThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(slots);
        if (completeThatch != null) {
            BlockRenderDispatcher dispatcher = dispatcher();
            return dispatcher != null && emitLegacyModel(world, pos, completeThatch,
                    ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch),
                    randomSupplier, context);
        }
        boolean emitted = false;
        for (int index = 0; index < Math.min(4, slots.length); index++) {
            BlockState slab = slot(slots, index);
            if (slab == null) {
                continue;
            }
            emitted |= emitPart(world, pos, slab, verticalStepPartState(index), ModelType.VERTICAL_STEP, verticalStepVisibleMask(index, slots), randomSupplier, context);
        }
        return emitted;
    }

    private boolean emitPart(BlockAndTintGetter world,
                             BlockPos pos,
                             BlockState slab,
                             BlockState partState,
                             ModelType modelType,
                             int visibleMask,
                             Supplier<RandomSource> randomSupplier,
                             RenderContext context) {
        return emitPart(world, pos, slab, partState, modelType, visibleMask, randomSupplier, context, quad -> quad);
    }

    private boolean emitPart(BlockAndTintGetter world,
                             BlockPos pos,
                             BlockState slab,
                             BlockState partState,
                             ModelType modelType,
                             int visibleMask,
                             Supplier<RandomSource> randomSupplier,
                             RenderContext context,
                             QuadMapper mapper) {
        BlockState normalized = prepareSourceState(slab);
        BlockRenderDispatcher dispatcher = dispatcher();
        if (normalized == null || normalized.isAir() || dispatcher == null || !rendersInCurrentPass(dispatcher, normalized, context, randomSupplier)) {
            return false;
        }

        RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(normalized);
        if (material == null) {
            return false;
        }

        boolean preserveSourceGeometry = SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(normalized.getBlock());
        BlockState geometryState = SlabFamilyModelSourceSelector.thatchGeometrySourceState(normalized, modelType);
        BakedModel sourceModel = dispatcher.getBlockModel(geometryState);
        BakedModel partModel = preserveSourceGeometry && modelType == ModelType.STEP
                ? EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true, null,
                        EnhancedSlabBlockBakedModel.topStepSourceModel(normalized, modelType, true))
                : partModel(sourceModel, modelType, preserveSourceGeometry);
        if (partModel == null) {
            return false;
        }

        boolean emitted = false;
        for (BakedQuad quad : partModel.getQuads(partState, null, randomSupplier.get())) {
            if (!isVisible(visibleMask, quad.getDirection())) {
                continue;
            }
            emitted |= EnhancedSlabFabricRenderUtil.emitQuad(context, mapper.map(quad), material, world, pos, normalized, null);
        }
        return emitted;
    }

    private boolean emitLegacyModel(BlockAndTintGetter world,
                                    BlockPos pos,
                                    BlockState slab,
                                    BakedModel model,
                                    Supplier<RandomSource> randomSupplier,
                                    RenderContext context) {
        BlockState normalized = prepareSourceState(slab);
        BlockRenderDispatcher dispatcher = dispatcher();
        if (model == null || normalized == null || dispatcher == null || !rendersInCurrentPass(dispatcher, normalized, context, randomSupplier)) {
            return false;
        }
        RenderMaterial material = EnhancedSlabFabricRenderUtil.materialFor(normalized);
        if (material == null) {
            return false;
        }
        boolean emitted = false;
        for (BakedQuad quad : model.getQuads(null, null, randomSupplier.get())) {
            emitted |= EnhancedSlabFabricRenderUtil.emitQuad(context, quad, material, world, pos, normalized, null);
        }
        return emitted;
    }

    private BakedModel partModel(BakedModel sourceModel, ModelType modelType, boolean preserveSourceGeometry) {
        if (sourceModel == null || modelType == ModelType.NONE) {
            return sourceModel;
        }
        if (preserveSourceGeometry) {
            return EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true);
        }
        synchronized (this.partModelCache) {
            EnumMap<ModelType, BakedModel> byType = this.partModelCache.computeIfAbsent(sourceModel, ignored -> new EnumMap<>(ModelType.class));
            return byType.computeIfAbsent(modelType, type -> EnhancedSlabBlockBakedModel.wrap(sourceModel, type));
        }
    }

    private static boolean rendersInCurrentPass(BlockRenderDispatcher dispatcher,
                                                BlockState slab,
                                                RenderContext context,
                                                Supplier<RandomSource> randomSupplier) {
        RenderType renderType = currentRenderType(context);
        if (renderType == null) {
            return true;
        }
        BakedModel model = dispatcher.getBlockModel(slab);
        if (model == null) {
            return true;
        }
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(slab, randomSupplier.get(), ModelData.EMPTY);
        return renderTypes == null || renderTypes.isEmpty() || renderTypes.contains(renderType);
    }

    private static RenderType currentRenderType(RenderContext context) {
        if (context == null) {
            return null;
        }
        try {
            Object value = context.getClass().getMethod("getRenderType").invoke(context);
            return value instanceof RenderType renderType ? renderType : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private void emitFallback(BlockAndTintGetter world,
                              BlockState state,
                              BlockPos pos,
                              Supplier<RandomSource> randomSupplier,
                              RenderContext context) {
        if (this.originalModel instanceof FabricBakedModel fabricModel) {
            fabricModel.emitBlockQuads(world, state, pos, randomSupplier, context);
        } else if (context != null) {
            context.bakedModelConsumer().accept(this.originalModel, state);
        }
    }

    static BlockState verticalSlabPartState(Direction facing) {
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

    private static BlockState stepPartState(BlockState state) {
        Direction facing = state != null && state.hasProperty(StepBlock.FACING) ? state.getValue(StepBlock.FACING) : Direction.NORTH;
        boolean top = state != null && state.hasProperty(StepBlock.TOP) && state.getValue(StepBlock.TOP);
        return stepPartState(facing, top);
    }

    static BlockState stepPartState(BlockState quadState, int index) {
        Direction.Axis axis = quadState != null && quadState.hasProperty(QuadStepBlock.AXIS)
                ? quadState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        return stepPartState(stepFacing(axis, index), index >= 2);
    }

    static BlockState stepPartState(Direction facing, boolean top) {
        BlockState state = CommonBlocks.STEP != null ? CommonBlocks.STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(StepBlock.FACING)) {
            state = state.setValue(StepBlock.FACING, facing);
        }
        if (state.hasProperty(StepBlock.TOP)) {
            state = state.setValue(StepBlock.TOP, top);
        }
        return state;
    }

    private static BlockState verticalStepPartState(BlockState state) {
        Direction facing = state != null && state.hasProperty(VerticalStepBlock.FACING) ? state.getValue(VerticalStepBlock.FACING) : Direction.NORTH;
        Direction side = state != null && state.hasProperty(VerticalStepBlock.SIDE) ? state.getValue(VerticalStepBlock.SIDE) : Direction.WEST;
        return verticalStepPartState(facing, side);
    }

    static BlockState verticalStepPartState(int index) {
        return verticalStepPartState(index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH, index == 1 || index == 3 ? Direction.EAST : Direction.WEST);
    }

    static BlockState verticalStepPartState(Direction facing, Direction side) {
        BlockState state = CommonBlocks.VERTICAL_STEP != null ? CommonBlocks.VERTICAL_STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(VerticalStepBlock.FACING)) {
            state = state.setValue(VerticalStepBlock.FACING, facing);
        }
        if (state.hasProperty(VerticalStepBlock.SIDE)) {
            state = state.setValue(VerticalStepBlock.SIDE, side);
        }
        return state;
    }

    static int stepVisibleMask(BlockState quadState, int index, BlockState[] slabs) {
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

    static int verticalStepVisibleMask(int index, BlockState[] slabs) {
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

    private static Direction stepFacing(Direction.Axis axis, int index) {
        boolean positive = index == 1 || index == 3;
        return axis == Direction.Axis.Z
                ? (positive ? Direction.SOUTH : Direction.NORTH)
                : (positive ? Direction.EAST : Direction.WEST);
    }

    private static BlockState prepareSourceState(BlockState state) {
        return EnhancedSlabFabricRenderUtil.normalizeSlabState(state);
    }

    private static BlockState slot(BlockState[] slabs, int index) {
        return slabs != null && index >= 0 && index < slabs.length ? prepareSourceState(slabs[index]) : null;
    }

    private static boolean hasAnySlot(BlockState[] slabs) {
        if (slabs == null) {
            return false;
        }
        for (BlockState slab : slabs) {
            BlockState normalized = prepareSourceState(slab);
            if (normalized != null && !normalized.isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
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

    private static BlockRenderDispatcher dispatcher() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getBlockRenderer();
    }

    @FunctionalInterface
    private interface QuadMapper {
        BakedQuad map(BakedQuad quad);
    }
}
