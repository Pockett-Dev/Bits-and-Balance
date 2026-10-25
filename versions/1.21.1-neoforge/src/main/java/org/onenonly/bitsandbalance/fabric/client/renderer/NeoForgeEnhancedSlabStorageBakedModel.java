package org.onenonly.bitsandbalance.fabric.client.renderer;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabBlockBakedModel;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;

public final class NeoForgeEnhancedSlabStorageBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {
    private static final ModelProperty<BlockState[]> MATERIAL_SLOTS = new ModelProperty<>();
    private static final ModelProperty<BlockState> RENDER_STATE = new ModelProperty<>();
    private static final ModelProperty<Kind> RENDER_KIND = new ModelProperty<>();

    private final Kind kind;
    private final Map<BakedModel, EnumMap<EnhancedSlabItemSupport.ModelType, BakedModel>> partModelCache =
            Collections.synchronizedMap(new WeakHashMap<>());

    private NeoForgeEnhancedSlabStorageBakedModel(BakedModel originalModel, Kind kind) {
        super(originalModel);
        this.kind = kind;
    }

    public static BakedModel wrap(BakedModel originalModel, Kind kind) {
        return originalModel == null ? null : new NeoForgeEnhancedSlabStorageBakedModel(originalModel, kind);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return side == null ? this.originalModel.getQuads(state, side, safeRandom(random)) : List.of();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData modelData, RenderType renderType) {
        if (!Config.enableEnhancedSlabs) {
            return side == null ? this.originalModel.getQuads(state, side, safeRandom(random), modelData, renderType) : List.of();
        }

        BlockState[] slots = slots(modelData);
        if (!hasAnySlot(slots)) {
            return this.originalModel.getQuads(state, side, safeRandom(random), modelData, renderType);
        }

        List<BakedQuad> quads = buildQuads(renderKind(modelData), renderState(modelData, state), slots, renderType, random);
        return useSodiumFaceBuckets() ? filteredQuads(quads, side) : side == null ? quads : List.of();
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData base = modelData == null ? ModelData.EMPTY : modelData;
        RenderData renderData = collectRenderData(level, pos, state);
        return renderData != null && hasAnySlot(renderData.slots())
                ? base.derive()
                        .with(MATERIAL_SLOTS, renderData.slots())
                        .with(RENDER_STATE, renderData.state())
                        .with(RENDER_KIND, renderData.kind())
                        .build()
                : base;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        BlockState[] slots = slots(modelData);
        if (!hasAnySlot(slots)) {
            return this.originalModel.getRenderTypes(state, safeRandom(random), modelData);
        }

        BlockRenderDispatcher dispatcher = dispatcher();
        if (dispatcher == null) {
            return this.originalModel.getRenderTypes(state, safeRandom(random), modelData);
        }

        List<ChunkRenderTypeSet> renderTypes = new ArrayList<>(slots.length);
        for (BlockState slot : slots) {
            BlockState normalized = normalizeSlot(slot);
            if (normalized == null) {
                continue;
            }

            BakedModel model = dispatcher.getBlockModel(normalized);
            if (model != null) {
                ChunkRenderTypeSet set = model.getRenderTypes(normalized, safeRandom(random), ModelData.EMPTY);
                if (set != null && !set.isEmpty()) {
                    renderTypes.add(set);
                }
            }
        }
        return renderTypes.isEmpty()
                ? this.originalModel.getRenderTypes(state, safeRandom(random), modelData)
                : ChunkRenderTypeSet.union(renderTypes);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData modelData) {
        BlockState[] slots = slots(modelData);
        BlockRenderDispatcher dispatcher = dispatcher();
        if (dispatcher != null && slots != null) {
            for (BlockState slot : slots) {
                BlockState normalized = normalizeSlot(slot);
                if (normalized != null) {
                    return dispatcher.getBlockModel(normalized).getParticleIcon();
                }
            }
        }
        return this.originalModel.getParticleIcon(modelData);
    }

    private List<BakedQuad> buildQuads(Kind kind, BlockState ownerState, BlockState[] slots, RenderType renderType, RandomSource random) {
        BlockRenderDispatcher dispatcher = dispatcher();
        if (dispatcher == null) {
            return List.of();
        }

        List<BakedQuad> quads = new ArrayList<>(24);
        switch (kind == null ? this.kind : kind) {
            case MIXED_SLAB -> appendMixedSlab(dispatcher, quads, slots, renderType, random);
            case VERTICAL_SLAB -> appendVerticalSlab(dispatcher, quads, ownerState, slots, renderType, random);
            case STEP -> appendStep(dispatcher, quads, ownerState, slot(slots, 0), renderType, random);
            case QUAD_STEP -> appendQuadStep(dispatcher, quads, ownerState, slots, renderType, random);
            case VERTICAL_STEP -> appendVerticalStep(dispatcher, quads, ownerState, slot(slots, 0), renderType, random);
            case QUAD_VERTICAL_STEP -> appendQuadVerticalStep(dispatcher, quads, slots, renderType, random);
        }
        return quads.isEmpty() ? List.of() : List.copyOf(quads);
    }

    private void appendMixedSlab(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState[] slots, RenderType renderType, RandomSource random) {
        BlockState bottom = normalizeSlot(slot(slots, 0));
        BlockState top = normalizeSlot(slot(slots, 1));
        BlockState fullThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(bottom, top);
        if (fullThatch != null) {
            if (shouldRenderSlot(dispatcher, fullThatch, renderType, random)) {
                appendModel(quads, ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(fullThatch), fullThatch));
            }
            return;
        }
        if (shouldRenderSlot(dispatcher, bottom, renderType, random)) {
            appendModel(quads, LegacyEnhancedSlabRenderHelper.createHorizontalSlabModel(dispatcher, bottom, SlabType.BOTTOM, top != null ? Direction.UP : null));
        }
        if (shouldRenderSlot(dispatcher, top, renderType, random)) {
            appendModel(quads, LegacyEnhancedSlabRenderHelper.createHorizontalSlabModel(dispatcher, top, SlabType.TOP, bottom != null ? Direction.DOWN : null));
        }
    }

    private void appendVerticalSlab(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState ownerState, BlockState[] slots, RenderType renderType, RandomSource random) {
        Direction facing = ownerState != null && ownerState.hasProperty(VerticalSlabBlock.FACING) ? ownerState.getValue(VerticalSlabBlock.FACING) : Direction.NORTH;
        boolean doubled = ownerState != null && ownerState.hasProperty(VerticalSlabBlock.DOUBLE) && ownerState.getValue(VerticalSlabBlock.DOUBLE);
        BlockState facingSlab = normalizeSlot(slot(slots, 0));
        BlockState oppositeSlab = normalizeSlot(slot(slots, 1));

        BlockState completeThatch = doubled
                ? SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab)
                : null;
        if (completeThatch != null) {
            if (shouldRenderSlot(dispatcher, completeThatch, renderType, random)) {
                appendModel(quads, ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch));
            }
            return;
        }

        if (shouldRenderSlot(dispatcher, facingSlab, renderType, random)) {
            int mask = doubled && oppositeSlab != null
                    ? LegacyEnhancedSlabRenderHelper.without(LegacyEnhancedSlabRenderHelper.allDirectionMask(), facing.getOpposite())
                    : LegacyEnhancedSlabRenderHelper.allDirectionMask();
            appendPart(
                    dispatcher,
                    quads,
                    facingSlab,
                    verticalSlabPartState(facing),
                    EnhancedSlabItemSupport.ModelType.VERTICAL_SLAB,
                    mask,
                    UnaryOperator.identity()
            );
        }
        if (doubled && shouldRenderSlot(dispatcher, oppositeSlab, renderType, random)) {
            int mask = facingSlab != null
                    ? LegacyEnhancedSlabRenderHelper.without(LegacyEnhancedSlabRenderHelper.allDirectionMask(), facing)
                    : LegacyEnhancedSlabRenderHelper.allDirectionMask();
            appendPart(
                    dispatcher,
                    quads,
                    oppositeSlab,
                    verticalSlabPartState(facing.getOpposite()),
                    EnhancedSlabItemSupport.ModelType.VERTICAL_SLAB,
                    mask,
                    UnaryOperator.identity()
            );
        }
    }

    private void appendStep(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState ownerState, BlockState slab, RenderType renderType, RandomSource random) {
        BlockState normalized = normalizeSlot(slab);
        if (!shouldRenderSlot(dispatcher, normalized, renderType, random)) {
            return;
        }

        Direction facing = ownerState != null && ownerState.hasProperty(StepBlock.FACING) ? ownerState.getValue(StepBlock.FACING) : Direction.NORTH;
        boolean top = ownerState != null && ownerState.hasProperty(StepBlock.TOP) && ownerState.getValue(StepBlock.TOP);
        appendPart(
                dispatcher,
                quads,
                normalized,
                stepPartState(facing, top),
                EnhancedSlabItemSupport.ModelType.STEP,
                LegacyEnhancedSlabRenderHelper.allDirectionMask(),
                UnaryOperator.identity()
        );
    }

    private void appendQuadStep(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState ownerState, BlockState[] slots, RenderType renderType, RandomSource random) {
        BlockState completeThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(slots);
        if (completeThatch != null) {
            if (shouldRenderSlot(dispatcher, completeThatch, renderType, random)) {
                appendModel(quads, ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch));
            }
            return;
        }
        BlockState[] stackedThatch = new BlockState[2];
        for (int bottomIndex = 0; bottomIndex < 2; bottomIndex++) {
            BlockState fullState = ThatchHalfBlockBakedModel.fullStateForStackedPair(slots, bottomIndex);
            if (fullState != null) {
                stackedThatch[bottomIndex] = fullState;
                if (shouldRenderSlot(dispatcher, fullState, renderType, random)) {
                    Direction.Axis axis = ownerState != null && ownerState.hasProperty(QuadStepBlock.AXIS)
                            ? ownerState.getValue(QuadStepBlock.AXIS) : Direction.Axis.X;
                    BakedModel fullModel = dispatcher.getBlockModel(fullState);
                    appendModel(quads, ThatchHalfBlockBakedModel.wrap(fullModel, fullState, stepFacing(axis, bottomIndex)));
                }
            }
        }
        for (int index = 0; index < 4; index++) {
            if (stackedThatch[index & 1] != null) {
                continue;
            }
            BlockState slab = normalizeSlot(slot(slots, index));
            if (!shouldRenderSlot(dispatcher, slab, renderType, random)) {
                continue;
            }

            int slotIndex = index;
            appendPart(
                    dispatcher,
                    quads,
                    slab,
                    stepPartState(ownerState, slotIndex),
                    EnhancedSlabItemSupport.ModelType.STEP,
                    stepVisibleMask(ownerState, slotIndex, slots),
                    UnaryOperator.identity()
            );
        }
    }

    private void appendVerticalStep(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState ownerState, BlockState slab, RenderType renderType, RandomSource random) {
        BlockState normalized = normalizeSlot(slab);
        if (!shouldRenderSlot(dispatcher, normalized, renderType, random)) {
            return;
        }

        Direction facing = ownerState != null && ownerState.hasProperty(VerticalStepBlock.FACING) ? ownerState.getValue(VerticalStepBlock.FACING) : Direction.NORTH;
        Direction side = ownerState != null && ownerState.hasProperty(VerticalStepBlock.SIDE) ? ownerState.getValue(VerticalStepBlock.SIDE) : Direction.WEST;
        appendPart(
                dispatcher,
                quads,
                normalized,
                verticalStepPartState(facing, side),
                EnhancedSlabItemSupport.ModelType.VERTICAL_STEP,
                LegacyEnhancedSlabRenderHelper.allDirectionMask(),
                UnaryOperator.identity()
        );
    }

    private void appendQuadVerticalStep(BlockRenderDispatcher dispatcher, List<BakedQuad> quads, BlockState[] slots, RenderType renderType, RandomSource random) {
        BlockState completeThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(slots);
        if (completeThatch != null) {
            if (shouldRenderSlot(dispatcher, completeThatch, renderType, random)) {
                appendModel(quads, ThatchHalfBlockBakedModel.wrapFull(dispatcher.getBlockModel(completeThatch), completeThatch));
            }
            return;
        }
        for (int index = 0; index < 4; index++) {
            BlockState slab = normalizeSlot(slot(slots, index));
            if (!shouldRenderSlot(dispatcher, slab, renderType, random)) {
                continue;
            }

            appendPart(
                    dispatcher,
                    quads,
                    slab,
                    verticalStepPartState(index),
                    EnhancedSlabItemSupport.ModelType.VERTICAL_STEP,
                    verticalStepVisibleMask(index, slots),
                    UnaryOperator.identity()
            );
        }
    }

    private void appendPart(BlockRenderDispatcher dispatcher,
                            List<BakedQuad> quads,
                            BlockState materialState,
                            BlockState partState,
                            EnhancedSlabItemSupport.ModelType modelType,
                            int visibleMask,
                            UnaryOperator<BakedQuad> mapper) {
        boolean preserveSourceGeometry = SlabFamilyModelSourceSelector.isUpgradeAquaticThatchSlab(materialState.getBlock());
        BlockState geometryState = SlabFamilyModelSourceSelector.thatchGeometrySourceState(materialState, modelType);
        BakedModel sourceModel = dispatcher.getBlockModel(geometryState);
        if (preserveSourceGeometry && modelType == EnhancedSlabItemSupport.ModelType.STEP) {
            BakedModel topSourceModel = EnhancedSlabBlockBakedModel.topStepSourceModel(materialState, modelType, true);
            BakedModel partModel = EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true, null, topSourceModel);
            appendVisibleQuads(quads, partModel, partState, visibleMask, mapper);
            return;
        }
        BakedModel partModel = partModel(sourceModel, modelType, preserveSourceGeometry);
        if (partModel == null) {
            return;
        }

        appendVisibleQuads(quads, partModel, partState, visibleMask, mapper);
    }

    private static void appendVisibleQuads(List<BakedQuad> quads, BakedModel partModel, BlockState partState,
                                           int visibleMask, UnaryOperator<BakedQuad> mapper) {
        for (BakedQuad quad : partModel.getQuads(partState, null, seededRandom())) {
            Direction direction = quad == null ? null : quad.getDirection();
            if (quad == null || (direction != null && (visibleMask & (1 << direction.ordinal())) == 0)) {
                continue;
            }
            BakedQuad mapped = mapper.apply(quad);
            if (mapped != null) {
                quads.add(mapped);
            }
        }
    }

    private BakedModel partModel(BakedModel sourceModel, EnhancedSlabItemSupport.ModelType modelType, boolean preserveSourceGeometry) {
        if (sourceModel == null || modelType == EnhancedSlabItemSupport.ModelType.NONE) {
            return sourceModel;
        }
        if (preserveSourceGeometry) {
            return EnhancedSlabBlockBakedModel.wrap(sourceModel, modelType, true);
        }
        synchronized (this.partModelCache) {
            EnumMap<EnhancedSlabItemSupport.ModelType, BakedModel> byType = this.partModelCache.computeIfAbsent(
                    sourceModel,
                    ignored -> new EnumMap<>(EnhancedSlabItemSupport.ModelType.class)
            );
            return byType.computeIfAbsent(modelType, type -> EnhancedSlabBlockBakedModel.wrap(sourceModel, type));
        }
    }

    private static BlockState verticalSlabPartState(Direction facing) {
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

    private static BlockState stepPartState(BlockState quadState, int index) {
        Direction.Axis axis = quadState != null && quadState.hasProperty(QuadStepBlock.AXIS)
                ? quadState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        return stepPartState(stepFacing(axis, index), index >= 2);
    }

    private static BlockState stepPartState(Direction facing, boolean top) {
        BlockState state = CommonBlocks.STEP != null ? CommonBlocks.STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(StepBlock.FACING)) {
            state = state.setValue(StepBlock.FACING, facing);
        }
        if (state.hasProperty(StepBlock.TOP)) {
            state = state.setValue(StepBlock.TOP, top);
        }
        return state;
    }

    private static BlockState verticalStepPartState(int index) {
        return verticalStepPartState(
                index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH,
                index == 1 || index == 3 ? Direction.EAST : Direction.WEST
        );
    }

    private static BlockState verticalStepPartState(Direction facing, Direction side) {
        BlockState state = CommonBlocks.VERTICAL_STEP != null ? CommonBlocks.VERTICAL_STEP.defaultBlockState() : Blocks.STONE.defaultBlockState();
        if (state.hasProperty(VerticalStepBlock.FACING)) {
            state = state.setValue(VerticalStepBlock.FACING, facing);
        }
        if (state.hasProperty(VerticalStepBlock.SIDE)) {
            state = state.setValue(VerticalStepBlock.SIDE, side);
        }
        return state;
    }

    private static int stepVisibleMask(BlockState quadState, int index, BlockState[] slabs) {
        Direction.Axis axis = quadState != null && quadState.hasProperty(QuadStepBlock.AXIS)
                ? quadState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        int mask = LegacyEnhancedSlabRenderHelper.allDirectionMask();
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = LegacyEnhancedSlabRenderHelper.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, verticalNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int verticalStepVisibleMask(int index, BlockState[] slabs) {
        int mask = LegacyEnhancedSlabRenderHelper.allDirectionMask();
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, eastWestNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, northSouthNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static Direction stepFacing(Direction.Axis axis, int index) {
        boolean positive = index == 1 || index == 3;
        return axis == Direction.Axis.Z
                ? (positive ? Direction.SOUTH : Direction.NORTH)
                : (positive ? Direction.EAST : Direction.WEST);
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        BlockState normalizedFirst = normalizeSlot(first);
        BlockState normalizedSecond = normalizeSlot(second);
        return normalizedFirst != null && normalizedFirst.equals(normalizedSecond);
    }

    @Nullable RenderData collectRenderData(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null) {
            return null;
        }

        EnhancedSlabMaterialSavedData.MaterialView savedSlots = materialView(level, pos);
        BlockState[] slots = mergeSlots(
                savedSlots,
                blockEntitySlots(level.getBlockEntity(pos)),
                createRenderSlots(level, pos)
        );
        RenderData predicted = predictedRenderData(level, pos, state, slots);
        if (predicted != null && hasAnySlot(predicted.slots())) {
            return predicted;
        }

        return hasAnySlot(slots) ? new RenderData(this.kind, state, slots) : null;
    }

    private static @Nullable RenderData predictedRenderData(BlockAndTintGetter level, BlockPos pos, BlockState state, @Nullable BlockState[] actualSlots) {
        if (state == null) {
            return null;
        }

        if (state.getBlock() instanceof StepBlock) {
            EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction =
                    EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
            if (quadPrediction != null) {
                return new RenderData(Kind.QUAD_STEP, quadPrediction.quadState(), quadPrediction.slabs());
            }

            EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction =
                    EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
            if (singlePrediction != null) {
                return new RenderData(Kind.STEP, singlePrediction.stepState(), new BlockState[]{singlePrediction.slabState()});
            }
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction =
                    EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
            if (singlePrediction != null) {
                return new RenderData(Kind.STEP, singlePrediction.stepState(), new BlockState[]{singlePrediction.slabState()});
            }

            EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction =
                    EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
            if (quadPrediction != null) {
                if (countSlots(actualSlots) < 2) {
                    return new RenderData(Kind.QUAD_STEP, quadPrediction.quadState(), quadPrediction.slabs());
                }
            }
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction =
                    EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
            if (quadPrediction != null) {
                return new RenderData(Kind.QUAD_VERTICAL_STEP, quadPrediction.quadState(), quadPrediction.slabs());
            }

            EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction =
                    EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
            if (singlePrediction != null) {
                return new RenderData(Kind.VERTICAL_STEP, singlePrediction.stepState(), new BlockState[]{singlePrediction.slabState()});
            }
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction =
                    EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
            if (singlePrediction != null) {
                return new RenderData(Kind.VERTICAL_STEP, singlePrediction.stepState(), new BlockState[]{singlePrediction.slabState()});
            }

            EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction =
                    EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
            if (quadPrediction != null) {
                if (countSlots(actualSlots) < 2) {
                    return new RenderData(Kind.QUAD_VERTICAL_STEP, quadPrediction.quadState(), quadPrediction.slabs());
                }
            }
        }

        return null;
    }

    private static @Nullable BlockState[] blockEntitySlots(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return normalizeSlots(new BlockState[]{verticalSlab.getFacingSlab(), verticalSlab.getOppositeSlab()});
        }
        if (blockEntity instanceof StepBlockEntity step) {
            return normalizeSlots(new BlockState[]{step.getSlabState()});
        }
        if (blockEntity instanceof QuadStepBlockEntity quadStep) {
            return normalizeSlots(quadStep.getAllSlabs());
        }
        if (blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            return normalizeSlots(new BlockState[]{verticalStep.getSlabState()});
        }
        if (blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return normalizeSlots(quadVerticalStep.getAllSlabs());
        }
        return null;
    }

    private static @Nullable EnhancedSlabMaterialSavedData.MaterialView materialView(BlockAndTintGetter level, BlockPos pos) {
        if (level instanceof Level actualLevel) {
            EnhancedSlabMaterialSavedData.MaterialView stored = EnhancedSlabMaterialSavedData.getView(actualLevel, pos.asLong());
            if (stored != null) {
                return stored;
            }
        }
        return EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
    }

    private static @Nullable BlockState[] createRenderSlots(BlockAndTintGetter level, BlockPos pos) {
        BlockState[] createSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        return createSlots != null ? normalizeSlots(createSlots) : null;
    }

    private static @Nullable BlockState[] mergeSlots(@Nullable EnhancedSlabMaterialSavedData.MaterialView stored,
                                                     @Nullable BlockState[] blockEntity,
                                                     @Nullable BlockState[] create) {
        BlockState[] merged = new BlockState[4];
        for (int i = 0; i < merged.length; i++) {
            merged[i] = normalizeSlot(stored == null ? null : stored.slot(i));
            if (merged[i] == null) {
                merged[i] = normalizeSlot(slot(blockEntity, i));
            }
            if (merged[i] == null) {
                merged[i] = normalizeSlot(slot(create, i));
            }
        }
        return hasAnySlot(merged) ? merged : null;
    }

    private static @Nullable BlockState[] slots(ModelData modelData) {
        return modelData != null && modelData.has(MATERIAL_SLOTS) ? modelData.get(MATERIAL_SLOTS) : null;
    }

    private static BlockState renderState(ModelData modelData, BlockState fallback) {
        BlockState state = modelData != null && modelData.has(RENDER_STATE) ? modelData.get(RENDER_STATE) : null;
        return state == null ? fallback : state;
    }

    private Kind renderKind(ModelData modelData) {
        Kind kind = modelData != null && modelData.has(RENDER_KIND) ? modelData.get(RENDER_KIND) : null;
        return kind == null ? this.kind : kind;
    }

    private static @Nullable BlockState[] normalizeSlots(@Nullable BlockState[] slots) {
        if (slots == null) {
            return null;
        }

        BlockState[] normalized = new BlockState[slots.length];
        boolean any = false;
        for (int i = 0; i < slots.length; i++) {
            normalized[i] = normalizeSlot(slots[i]);
            any |= normalized[i] != null;
        }
        return any ? normalized : null;
    }

    private static @Nullable BlockState normalizeSlot(@Nullable BlockState slot) {
        return DynamicVariantMaterialHelper.normalizeSlabState(slot);
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static boolean hasAnySlot(@Nullable BlockState[] slots) {
        if (slots == null) {
            return false;
        }
        for (BlockState slot : slots) {
            if (slot != null) {
                return true;
            }
        }
        return false;
    }

    private static int countSlots(@Nullable BlockState[] slots) {
        int count = 0;
        if (slots != null) {
            for (BlockState slot : slots) {
                if (slot != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean shouldRenderSlot(BlockRenderDispatcher dispatcher, @Nullable BlockState slot, RenderType renderType, RandomSource random) {
        if (slot == null) {
            return false;
        }
        if (renderType == null) {
            return true;
        }

        BakedModel model = dispatcher.getBlockModel(slot);
        if (model == null) {
            return true;
        }
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(slot, safeRandom(random), ModelData.EMPTY);
        return renderTypes == null || renderTypes.isEmpty() || renderTypes.contains(renderType);
    }

    private static void appendModel(List<BakedQuad> quads, BakedModel model) {
        if (model != null) {
            quads.addAll(model.getQuads(null, null, seededRandom()));
        }
    }

    private static List<BakedQuad> filteredQuads(List<BakedQuad> quads, Direction side) {
        if (quads == null || quads.isEmpty()) {
            return List.of();
        }
        if (side == null) {
            return List.of();
        }

        List<BakedQuad> filtered = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (quad != null && quad.getDirection() == side) {
                filtered.add(quad);
            }
        }
        return filtered.isEmpty() ? List.of() : List.copyOf(filtered);
    }

    private static boolean useSodiumFaceBuckets() {
        return ModList.get().isLoaded("sodium");
    }

    private static BlockRenderDispatcher dispatcher() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getBlockRenderer();
    }

    private static RandomSource safeRandom(RandomSource random) {
        return random == null ? seededRandom() : random;
    }

    private static RandomSource seededRandom() {
        RandomSource random = RandomSource.create();
        random.setSeed(42L);
        return random;
    }

    public enum Kind {
        MIXED_SLAB,
        VERTICAL_SLAB,
        STEP,
        QUAD_STEP,
        VERTICAL_STEP,
        QUAD_VERTICAL_STEP
    }

    record RenderData(Kind kind, BlockState state, BlockState[] slots) {
    }
}
