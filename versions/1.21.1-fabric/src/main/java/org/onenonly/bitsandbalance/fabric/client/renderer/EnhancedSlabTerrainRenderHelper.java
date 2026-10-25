package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mixin.EnhancedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

public final class EnhancedSlabTerrainRenderHelper {
    private static final BlockState[] EMPTY_SLOTS = new BlockState[4];
    private static final int ALL_DIRECTIONS = LegacyEnhancedSlabRenderHelper.allDirectionMask();

    private EnhancedSlabTerrainRenderHelper() {
    }

    public static boolean isEnabled() {
        return FabricTweaksConfig.enableEnhancedSlabs;
    }

    public static boolean handles(BlockState state) {
        return state != null
                && (state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock);
    }

    public static Direction mixedBottomHiddenFace(BlockRenderDispatcher dispatcher, BlockState bottomSlabState, BlockState topSlabState) {
        return LegacyEnhancedSlabRenderHelper.mixedBottomHiddenFace(dispatcher, bottomSlabState, topSlabState);
    }

    public static boolean render(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (!isEnabled()) {
            return false;
        }
        if (state.getBlock() instanceof MixedSlabBlock) {
            return renderMixedSlab(dispatcher, level, pos, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
            return renderFixedVerticalSlab(dispatcher, level, pos, state, fixedVerticalSlab, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof VerticalSlabBlock) {
            return renderVerticalSlab(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof FixedStepBlock fixedStep) {
            return renderFixedStep(dispatcher, level, pos, state, fixedStep, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof StepBlock) {
            return renderStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof QuadStepBlock) {
            // 1.21.1 quad containers have empty terrain models; their block entity renderer owns placed-block visuals.
            return false;
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return renderFixedVerticalStep(dispatcher, level, pos, state, fixedVerticalStep, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof VerticalStepBlock) {
            return renderVerticalStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            // 1.21.1 quad containers have empty terrain models; their block entity renderer owns placed-block visuals.
            return false;
        }
        return false;
    }

    public static boolean renderCreateContraption(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        if (!isEnabled() || dispatcher == null || level == null || pos == null || state == null || poseStack == null || vertexConsumer == null) {
            return false;
        }
        if (state.getBlock() instanceof MixedSlabBlock) {
            return renderMixedSlab(dispatcher, level, pos, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof QuadStepBlock) {
            return renderQuadStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            return renderQuadVerticalStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        return render(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
    }

    public static boolean renderMoving(BlockRenderDispatcher dispatcher, Level level, BlockPos renderPos, BlockPos destPos, BlockState movedState, Object movingBlockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (movedState.getBlock() instanceof MixedSlabBlock) {
            BlockState bottom = movingBlockEntity instanceof MixedSlabMovingPistonAccess access ? access.bitsandbalance$getBottomSlab() : null;
            BlockState top = movingBlockEntity instanceof MixedSlabMovingPistonAccess access ? access.bitsandbalance$getTopSlab() : null;
            if (bottom == null || top == null) {
                MixedSlabPistonRenderCache.Entry entry = MixedSlabPistonRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
                if (entry != null) {
                    bottom = entry.bottomSlab();
                    top = entry.topSlab();
                }
            }
            if (bottom == null || top == null) {
                Object slabs = movingMaterialSlots(level, renderPos, destPos);
                if (bottom == null) {
                    bottom = materialSlot(slabs, 0);
                }
                if (top == null) {
                    top = materialSlot(slabs, 1);
                }
            }

            if (bottom == null && top == null) {
                return false;
            }

            if (bottom != null) {
                LegacyEnhancedSlabRenderHelper.renderHorizontalSlab(dispatcher, level, renderPos, bottom, SlabType.BOTTOM, mixedBottomHiddenFace(dispatcher, bottom, top), poseStack, buffer, 0, packedOverlay);
            }
            if (top != null) {
                LegacyEnhancedSlabRenderHelper.renderHorizontalSlab(dispatcher, level, renderPos, top, SlabType.TOP, bottom != null ? Direction.DOWN : null, poseStack, buffer, 0, packedOverlay);
            }
            return true;
        }

        if (!EnhancedSlabPistonHooks.supports(movedState)) {
            return false;
        }

        EnhancedSlabPistonData data = movingBlockEntity instanceof EnhancedSlabMovingPistonAccess access ? access.bitsandbalance$getEnhancedSlabPistonData() : null;
        if (data == null) {
            EnhancedSlabPistonRenderCache.Entry entry = EnhancedSlabPistonRenderCache.peekForMove(destPos.asLong(), renderPos.asLong());
            data = entry == null ? null : entry.data();
        }
        if (data == null) {
            data = pistonDataFromSavedSlots(movedState, movingMaterialSlots(level, renderPos, destPos));
        }
        if (data == null) {
            return false;
        }

        renderPistonData(dispatcher, level, renderPos, movedState, data, poseStack, buffer, packedOverlay);
        return true;
    }

    private static boolean renderMixedSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
        BlockState[] slabs = stored == null ? materialSlots(level, pos) : null;
        BlockState bottom = slot(slabs, stored, 0);
        BlockState top = slot(slabs, stored, 1);
        if (bottom == null && top == null) {
            return false;
        }

        boolean rendered = false;
        if (bottom != null) {
            LegacyEnhancedSlabRenderHelper.renderHorizontalSlabToBuffer(dispatcher, level, pos, bottom, SlabType.BOTTOM, mixedBottomHiddenFace(dispatcher, bottom, top), poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        if (top != null) {
            LegacyEnhancedSlabRenderHelper.renderHorizontalSlabToBuffer(dispatcher, level, pos, top, SlabType.TOP, bottom != null ? Direction.DOWN : null, poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderFixedVerticalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, FixedVerticalSlabBlock fixedVerticalSlab, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Block sourceSlab = fixedVerticalSlab.getSourceSlab();
        if (sourceSlab == null) {
            return false;
        }

        BlockState slabState = sourceSlab.defaultBlockState();
        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, slabState, facing, poseStack, vertexConsumer, packedOverlay);
        if (state.getValue(VerticalSlabBlock.DOUBLE)) {
            LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, slabState, facing.getOpposite(), poseStack, vertexConsumer, packedOverlay);
        }
        return true;
    }

    private static boolean renderVerticalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState facingSlab = null;
        BlockState oppositeSlab = null;
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            facingSlab = verticalSlab.getFacingSlab();
            oppositeSlab = verticalSlab.getOppositeSlab();
        } else {
            EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
            BlockState[] slabs = stored == null ? materialSlots(level, pos) : null;
            facingSlab = slot(slabs, stored, 0);
            oppositeSlab = slot(slabs, stored, 1);
        }
        if (facingSlab == null && oppositeSlab == null) {
            return false;
        }

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        boolean rendered = false;
        if (facingSlab != null) {
            LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, facingSlab, facing, poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        if (state.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
            LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, oppositeSlab, facing.getOpposite(), poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
        if (quadPrediction != null) {
            return renderQuadStepSlabs(dispatcher, level, pos, quadPrediction.quadState(), quadPrediction.slabs(), poseStack, vertexConsumer, packedOverlay);
        }

        EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        EnhancedSlabMaterialSavedData.MaterialView stored = blockEntity instanceof StepBlockEntity ? null : storedMaterialView(level, pos);
        BlockState slab = blockEntity instanceof StepBlockEntity step
                ? step.getSlabState()
                : slot(stored == null ? materialSlots(level, pos) : null, stored, 0);
        if (slab == null) {
            return false;
        }

        renderStepState(dispatcher, level, pos, state, slab, poseStack, vertexConsumer, packedOverlay);
        return true;
    }

    private static boolean renderFixedStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, FixedStepBlock fixedStep, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Block sourceSlab = fixedStep.getSourceSlab();
        if (sourceSlab == null) {
            return false;
        }

        renderStepState(dispatcher, level, pos, state, sourceSlab.defaultBlockState(), poseStack, vertexConsumer, packedOverlay);
        return true;
    }

    private static boolean renderQuadStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        Object actualSlabs = materialSource(level, pos, state);
        EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
        if (quadPrediction != null && countSlabs(actualSlabs) < 2) {
            return renderQuadStepSlabs(dispatcher, level, pos, quadPrediction.quadState(), quadPrediction.slabs(), poseStack, vertexConsumer, packedOverlay);
        }

        if (quadPrediction != null && level instanceof Level actualLevel) {
            EnhancedSlabPlacementPredictionCache.clearQuadStep(actualLevel, pos);
        }

        return renderQuadStepSlabs(dispatcher, level, pos, state, actualSlabs, poseStack, vertexConsumer, packedOverlay);
    }

    private static boolean renderQuadStepSlabs(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, Object slabs, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        boolean rendered = false;
        for (int index = 0; index < 4; index++) {
            BlockState slab = materialSlot(slabs, index);
            if (slab == null) {
                continue;
            }

            boolean positive = index == 1 || index == 3;
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            LegacyEnhancedSlabRenderHelper.renderStepToBuffer(dispatcher, level, pos, slab, facing, index >= 2, poseStack, vertexConsumer, packedOverlay, quadStepVisibleMask(axis, index, slabs));
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
        if (quadPrediction != null) {
            return renderQuadVerticalStepSlabs(dispatcher, level, pos, quadPrediction.slabs(), poseStack, vertexConsumer, packedOverlay);
        }

        EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderVerticalStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        EnhancedSlabMaterialSavedData.MaterialView stored = blockEntity instanceof VerticalStepBlockEntity ? null : storedMaterialView(level, pos);
        BlockState slab = blockEntity instanceof VerticalStepBlockEntity verticalStep
                ? verticalStep.getSlabState()
                : slot(stored == null ? materialSlots(level, pos) : null, stored, 0);
        if (slab == null) {
            return false;
        }

        renderVerticalStepState(dispatcher, level, pos, state, slab, poseStack, vertexConsumer, packedOverlay);
        return true;
    }

    private static boolean renderFixedVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, FixedVerticalStepBlock fixedVerticalStep, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
        if (sourceVerticalSlab == null) {
            return false;
        }

        renderVerticalStepState(dispatcher, level, pos, state, verticalStepSourceRenderState(sourceVerticalSlab), poseStack, vertexConsumer, packedOverlay);
        return true;
    }

    private static boolean renderQuadVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderVerticalStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        Object actualSlabs = materialSource(level, pos, state);
        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
        if (quadPrediction != null && countSlabs(actualSlabs) < 2) {
            return renderQuadVerticalStepSlabs(dispatcher, level, pos, quadPrediction.slabs(), poseStack, vertexConsumer, packedOverlay);
        }

        if (quadPrediction != null && level instanceof Level actualLevel) {
            EnhancedSlabPlacementPredictionCache.clearQuadVerticalStep(actualLevel, pos);
        }
        return renderQuadVerticalStepSlabs(dispatcher, level, pos, actualSlabs, poseStack, vertexConsumer, packedOverlay);
    }

    public static @Nullable BlockState[] materialSlots(BlockAndTintGetter level, BlockPos pos) {
        return materialSlots(level, pos, null);
    }

    public static @Nullable BlockState[] materialSlots(BlockAndTintGetter level, BlockPos pos, @Nullable BlockState state) {
        if (level == null || pos == null) {
            return null;
        }
        EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
        if (stored != null) {
            return stored.copySlots();
        }

        return fallbackMaterialSlots(level, pos, state);
    }

    public static @Nullable Object materialSource(BlockAndTintGetter level, BlockPos pos, @Nullable BlockState state) {
        EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
        return stored == null ? fallbackMaterialSlots(level, pos, state) : stored;
    }

    private static @Nullable BlockState[] fallbackMaterialSlots(BlockAndTintGetter level,
                                                                 BlockPos pos,
                                                                 @Nullable BlockState state) {
        if (level == null || pos == null) {
            return null;
        }

        BlockState[] migrated = blockEntityMaterialSlots(level, pos, state);
        if (migrated != null && hasAnySlot(migrated) && level instanceof Level actualLevel && actualLevel.isClientSide()) {
            EnhancedSlabMaterialSavedData.rememberClient(actualLevel, pos.asLong(), migrated);
        }
        if (migrated != null && hasAnySlot(migrated)) {
            return migrated;
        }

        BlockState[] createSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        return createSlots != null ? createSlots : null;
    }

    public static @Nullable EnhancedSlabMaterialSavedData.MaterialView storedMaterialView(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        if (level instanceof Level actualLevel) {
            return EnhancedSlabMaterialSavedData.getView(actualLevel, pos.asLong());
        }
        return EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] slots,
                                             @Nullable EnhancedSlabMaterialSavedData.MaterialView view,
                                             int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : (view == null ? null : view.slot(index));
    }

    private static @Nullable BlockState[] blockEntityMaterialSlots(BlockAndTintGetter level, BlockPos pos, @Nullable BlockState state) {
        if (state == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (state.getBlock() instanceof VerticalSlabBlock && blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            return new BlockState[]{
                    verticalSlab.getFacingSlab(),
                    state.getValue(VerticalSlabBlock.DOUBLE) ? verticalSlab.getOppositeSlab() : null,
                    null,
                    null
            };
        }
        if (state.getBlock() instanceof StepBlock && blockEntity instanceof StepBlockEntity step) {
            return new BlockState[]{step.getSlabState(), null, null, null};
        }
        if (state.getBlock() instanceof VerticalStepBlock && blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            return new BlockState[]{verticalStep.getSlabState(), null, null, null};
        }
        if (state.getBlock() instanceof QuadStepBlock && blockEntity instanceof QuadStepBlockEntity quadStep) {
            return quadStep.getAllSlabs();
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock && blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            return quadVerticalStep.getAllSlabs();
        }
        return null;
    }

    private static @Nullable Object movingMaterialSlots(Level level, BlockPos renderPos, BlockPos destPos) {
        Object slabs = materialSource(level, renderPos, null);
        if (slabs != null) {
            return slabs;
        }
        slabs = materialSource(level, destPos, null);
        if (slabs != null) {
            return slabs;
        }
        for (Direction direction : Direction.values()) {
            slabs = materialSource(level, renderPos.relative(direction), null);
            if (slabs != null) {
                return slabs;
            }
            slabs = materialSource(level, destPos.relative(direction), null);
            if (slabs != null) {
                return slabs;
            }
        }
        return null;
    }

    public static @Nullable BlockState materialSlot(@Nullable Object slabs, int index) {
        if (slabs instanceof EnhancedSlabMaterialSavedData.MaterialView view) {
            return view.slot(index);
        }
        if (slabs instanceof BlockState[] states) {
            return index >= 0 && index < states.length ? states[index] : null;
        }
        return null;
    }

    private static @Nullable EnhancedSlabPistonData pistonDataFromSavedSlots(BlockState movedState, @Nullable Object slabs) {
        if (movedState == null || slabs == null || countSlabs(slabs) == 0) {
            return null;
        }
        if (movedState.getBlock() instanceof VerticalSlabBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_SLAB, new BlockState[]{materialSlot(slabs, 0), materialSlot(slabs, 1)});
        }
        if (movedState.getBlock() instanceof StepBlock && materialSlot(slabs, 0) != null) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.STEP, new BlockState[]{materialSlot(slabs, 0)});
        }
        if (movedState.getBlock() instanceof VerticalStepBlock && materialSlot(slabs, 0) != null) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_STEP, new BlockState[]{materialSlot(slabs, 0)});
        }
        if (movedState.getBlock() instanceof QuadStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_STEP, new BlockState[]{materialSlot(slabs, 0), materialSlot(slabs, 1), materialSlot(slabs, 2), materialSlot(slabs, 3)});
        }
        if (movedState.getBlock() instanceof QuadVerticalStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_VERTICAL_STEP, new BlockState[]{materialSlot(slabs, 0), materialSlot(slabs, 1), materialSlot(slabs, 2), materialSlot(slabs, 3)});
        }
        return null;
    }

    private static boolean hasAnySlot(@Nullable BlockState[] slabs) {
        if (slabs == null) {
            return false;
        }
        for (BlockState slab : slabs) {
            if (slab != null) {
                return true;
            }
        }
        return false;
    }

    private static void renderStepState(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState stepState, BlockState slabState, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Direction facing = stepState.getValue(StepBlock.FACING);
        boolean top = stepState.getValue(StepBlock.TOP);
        LegacyEnhancedSlabRenderHelper.renderStepToBuffer(dispatcher, level, pos, slabState, facing, top, poseStack, vertexConsumer, packedOverlay);
    }

    private static void renderVerticalStepState(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState stepState, BlockState slabState, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        Direction facing = stepState.getValue(VerticalStepBlock.FACING);
        Direction side = stepState.getValue(VerticalStepBlock.SIDE);
        LegacyEnhancedSlabRenderHelper.renderVerticalStepToBuffer(dispatcher, level, pos, slabState, facing, side, poseStack, vertexConsumer, packedOverlay);
    }

    private static BlockState verticalStepSourceRenderState(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        return (sourceSlab == null ? sourceVerticalSlab : sourceSlab).defaultBlockState();
    }

    private static boolean renderQuadVerticalStepSlabs(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, Object slabs, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        boolean rendered = false;
        for (int index = 0; index < 4; index++) {
            BlockState slab = materialSlot(slabs, index);
            if (slab == null) {
                continue;
            }

            Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
            Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
            LegacyEnhancedSlabRenderHelper.renderVerticalStepToBuffer(dispatcher, level, pos, slab, facing, side, poseStack, vertexConsumer, packedOverlay, quadVerticalStepVisibleMask(index, slabs));
            rendered = true;
        }
        return rendered;
    }

    private static void renderPistonData(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState movedState, EnhancedSlabPistonData data, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        switch (data.kind()) {
            case VERTICAL_SLAB -> {
                if (!(movedState.getBlock() instanceof VerticalSlabBlock)) return;
                Direction facing = movedState.getValue(VerticalSlabBlock.FACING);
                LegacyEnhancedSlabRenderHelper.renderVerticalSlab(dispatcher, level, pos, data.slab(0), facing, poseStack, buffer, 0, packedOverlay);
                if (movedState.getValue(VerticalSlabBlock.DOUBLE)) {
                    LegacyEnhancedSlabRenderHelper.renderVerticalSlab(dispatcher, level, pos, data.slab(1), facing.getOpposite(), poseStack, buffer, 0, packedOverlay);
                }
            }
            case STEP -> {
                if (!(movedState.getBlock() instanceof StepBlock)) return;
                LegacyEnhancedSlabRenderHelper.renderStep(dispatcher, level, pos, data.slab(0), movedState.getValue(StepBlock.FACING), movedState.getValue(StepBlock.TOP), poseStack, buffer, 0, packedOverlay);
            }
            case VERTICAL_STEP -> {
                if (!(movedState.getBlock() instanceof VerticalStepBlock)) return;
                LegacyEnhancedSlabRenderHelper.renderVerticalStep(dispatcher, level, pos, data.slab(0), movedState.getValue(VerticalStepBlock.FACING), movedState.getValue(VerticalStepBlock.SIDE), poseStack, buffer, 0, packedOverlay);
            }
            case QUAD_STEP -> {
                    if (!(movedState.getBlock() instanceof QuadStepBlock)) return;
                    Direction.Axis axis = movedState.getValue(QuadStepBlock.AXIS);
                    for (int index = 0; index < 4; index++) {
                        BlockState slab = data.slab(index);
                        if (slab == null) continue;
                        boolean positive = index == 1 || index == 3;
                        Direction facing = axis == Direction.Axis.Z ? (positive ? Direction.SOUTH : Direction.NORTH) : (positive ? Direction.EAST : Direction.WEST);
                    LegacyEnhancedSlabRenderHelper.renderStep(dispatcher, level, pos, slab, facing, index >= 2, poseStack, buffer, 0, packedOverlay, quadStepVisibleMask(axis, index, data.slabs()));
                }
            }
            case QUAD_VERTICAL_STEP -> {
                if (!(movedState.getBlock() instanceof QuadVerticalStepBlock)) return;
                for (int index = 0; index < 4; index++) {
                    BlockState slab = data.slab(index);
                    if (slab == null) continue;
                    Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
                    Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
                    LegacyEnhancedSlabRenderHelper.renderVerticalStep(dispatcher, level, pos, slab, facing, side, poseStack, buffer, 0, packedOverlay, quadVerticalStepVisibleMask(index, data.slabs()));
                }
            }
        }
    }

    private static int countSlabs(Object slabs) {
        int count = 0;
        for (int index = 0; index < 4; index++) {
            if (materialSlot(slabs, index) != null) {
                count++;
            }
        }
        return count;
    }

    private static int quadStepVisibleMask(Direction.Axis axis, int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (sameRenderMaterial(materialSlot(slabs, index), materialSlot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = LegacyEnhancedSlabRenderHelper.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (sameRenderMaterial(materialSlot(slabs, index), materialSlot(slabs, verticalNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static int quadVerticalStepVisibleMask(int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (sameRenderMaterial(materialSlot(slabs, index), materialSlot(slabs, eastWestNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (sameRenderMaterial(materialSlot(slabs, index), materialSlot(slabs, northSouthNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }
}
