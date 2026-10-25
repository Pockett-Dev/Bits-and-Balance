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
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mechanics.MixedSlabPistonRenderCache;
import org.onenonly.bitsandbalance.common.mixin.EnhancedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.Config;

public final class EnhancedSlabTerrainRenderHelper {
    private EnhancedSlabTerrainRenderHelper() {
    }

    public static boolean isEnabled() {
        return Config.enableEnhancedSlabs;
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
            return renderQuadStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
            return renderFixedVerticalStep(dispatcher, level, pos, state, fixedVerticalStep, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof VerticalStepBlock) {
            return renderVerticalStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            return renderQuadVerticalStep(dispatcher, level, pos, state, poseStack, vertexConsumer, packedOverlay);
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
                    bottom = slot(slabs, 0);
                }
                if (top == null) {
                    top = slot(slabs, 1);
                }
            }

            if (bottom == null && top == null) {
                return false;
            }

            BlockState fullThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(bottom, top);
            if (fullThatch != null) {
                LegacyEnhancedSlabRenderHelper.renderSourceModel(dispatcher, level, renderPos, fullThatch, poseStack, buffer, packedOverlay);
                return true;
            }

            if (bottom != null) {
                LegacyEnhancedSlabRenderHelper.renderHorizontalSlab(dispatcher, level, renderPos, bottom, SlabType.BOTTOM, top != null ? Direction.UP : null, poseStack, buffer, 0, packedOverlay);
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
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] savedSlabs = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        EnhancedSlabMaterialSavedData.MaterialView savedView = savedSlabs == null ? storedMaterialView(level, pos) : null;
        BlockState bottom = null;
        BlockState top = null;
        bottom = slot(savedSlabs, savedView, 0);
        top = slot(savedSlabs, savedView, 1);
        if (bottom == null && top == null) {
            return false;
        }

        BlockState fullThatch = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(bottom, top);
        if (fullThatch != null) {
            LegacyEnhancedSlabRenderHelper.renderSourceModelToBuffer(dispatcher, level, pos, fullThatch, poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        boolean rendered = false;
        if (bottom != null) {
            LegacyEnhancedSlabRenderHelper.renderHorizontalSlabToBuffer(dispatcher, level, pos, bottom, SlabType.BOTTOM, top != null ? Direction.UP : null, poseStack, vertexConsumer, packedOverlay);
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
        if (state.getValue(VerticalSlabBlock.DOUBLE)) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(slabState, slabState);
            if (fullBlockState != null) {
                LegacyEnhancedSlabRenderHelper.renderSourceModelToBuffer(dispatcher, level, pos, fullBlockState, poseStack, vertexConsumer, packedOverlay);
                return true;
            }
        }
        LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, slabState, facing, poseStack, vertexConsumer, packedOverlay);
        if (state.getValue(VerticalSlabBlock.DOUBLE)) {
            LegacyEnhancedSlabRenderHelper.renderVerticalSlabToBuffer(dispatcher, level, pos, slabState, facing.getOpposite(), poseStack, vertexConsumer, packedOverlay);
        }
        return true;
    }

    private static boolean renderVerticalSlab(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] savedSlabs = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        EnhancedSlabMaterialSavedData.MaterialView savedView = savedSlabs == null ? storedMaterialView(level, pos) : null;
        BlockState facingSlab = null;
        BlockState oppositeSlab = null;
        facingSlab = slot(savedSlabs, savedView, 0);
        oppositeSlab = slot(savedSlabs, savedView, 1);
        if (blockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
            if (facingSlab == null) {
                facingSlab = verticalSlab.getFacingSlab();
            }
            if (oppositeSlab == null) {
                oppositeSlab = verticalSlab.getOppositeSlab();
            }
        }
        if (facingSlab == null && oppositeSlab == null) {
            return false;
        }

        if (state.getValue(VerticalSlabBlock.DOUBLE) && facingSlab != null && oppositeSlab != null) {
            BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab);
            if (fullBlockState != null) {
                LegacyEnhancedSlabRenderHelper.renderSourceModelToBuffer(dispatcher, level, pos, fullBlockState, poseStack, vertexConsumer, packedOverlay);
                return true;
            }
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
            return renderQuadStepSlabs(dispatcher, level, pos, quadPrediction.quadState(), quadPrediction.slabs(), null, poseStack, vertexConsumer, packedOverlay);
        }

        EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] savedSlabs = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        EnhancedSlabMaterialSavedData.MaterialView savedView = savedSlabs == null ? storedMaterialView(level, pos) : null;
        BlockState slab = slot(savedSlabs, savedView, 0);
        if (slab == null && blockEntity instanceof StepBlockEntity step) {
            slab = step.getSlabState();
        }
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

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Object savedSlabs = materialSource(level, pos);
        BlockState[] blockEntitySlabs = blockEntity instanceof QuadStepBlockEntity quadStep ? quadStep.getAllSlabs() : null;
        EnhancedSlabPlacementPredictionCache.QuadStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(level, pos);
        if (quadPrediction != null && countSlabs(savedSlabs, blockEntitySlabs) < 2) {
            return renderQuadStepSlabs(dispatcher, level, pos, quadPrediction.quadState(), quadPrediction.slabs(), null, poseStack, vertexConsumer, packedOverlay);
        }

        if (quadPrediction != null && level instanceof Level actualLevel) {
            EnhancedSlabPlacementPredictionCache.clearQuadStep(actualLevel, pos);
        }

        return renderQuadStepSlabs(dispatcher, level, pos, state, savedSlabs, blockEntitySlabs, poseStack, vertexConsumer, packedOverlay);
    }

    private static boolean renderQuadStepSlabs(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, Object slabs, @Nullable BlockState[] fallback, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                mergedSlot(slabs, fallback, 0), mergedSlot(slabs, fallback, 1),
                mergedSlot(slabs, fallback, 2), mergedSlot(slabs, fallback, 3));
        if (fullBlockState != null) {
            LegacyEnhancedSlabRenderHelper.renderSourceModelToBuffer(dispatcher, level, pos, fullBlockState, poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        BlockState stackedThatch0 = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                mergedSlot(slabs, fallback, 0), mergedSlot(slabs, fallback, 2));
        BlockState stackedThatch1 = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                mergedSlot(slabs, fallback, 1), mergedSlot(slabs, fallback, 3));
        boolean rendered = false;
        for (int bottomIndex = 0; bottomIndex < 2; bottomIndex++) {
            BlockState thatchFullState = bottomIndex == 0 ? stackedThatch0 : stackedThatch1;
            if (thatchFullState != null) {
                Direction half = axis == Direction.Axis.Z
                        ? (bottomIndex == 0 ? Direction.NORTH : Direction.SOUTH)
                        : (bottomIndex == 0 ? Direction.WEST : Direction.EAST);
                LegacyEnhancedSlabRenderHelper.renderThatchHalfBlockToBuffer(
                        dispatcher, level, pos, thatchFullState, half, poseStack, vertexConsumer, packedOverlay);
                rendered = true;
            }
        }
        for (int index = 0; index < 4; index++) {
            if ((index & 1) == 0 ? stackedThatch0 != null : stackedThatch1 != null) {
                continue;
            }
            BlockState slab = mergedSlot(slabs, fallback, index);
            if (slab == null) {
                continue;
            }

            boolean positive = index == 1 || index == 3;
            Direction facing = axis == Direction.Axis.Z
                    ? (positive ? Direction.SOUTH : Direction.NORTH)
                    : (positive ? Direction.EAST : Direction.WEST);
            LegacyEnhancedSlabRenderHelper.renderStepToBuffer(dispatcher, level, pos, slab, facing, index >= 2, poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        return rendered;
    }

    private static boolean renderVerticalStep(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
        if (quadPrediction != null) {
            return renderQuadVerticalStepSlabs(dispatcher, level, pos, quadPrediction.slabs(), null, poseStack, vertexConsumer, packedOverlay);
        }

        EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(level, pos);
        if (singlePrediction != null && singlePrediction.slabState() != null) {
            renderVerticalStepState(dispatcher, level, pos, singlePrediction.stepState(), singlePrediction.slabState(), poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] savedSlabs = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        EnhancedSlabMaterialSavedData.MaterialView savedView = savedSlabs == null ? storedMaterialView(level, pos) : null;
        BlockState slab = slot(savedSlabs, savedView, 0);
        if (slab == null && blockEntity instanceof VerticalStepBlockEntity verticalStep) {
            slab = verticalStep.getSlabState();
        }
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

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Object savedSlabs = materialSource(level, pos);
        BlockState[] blockEntitySlabs = blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep ? quadVerticalStep.getAllSlabs() : null;
        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction quadPrediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(level, pos);
        if (quadPrediction != null && countSlabs(savedSlabs, blockEntitySlabs) < 2) {
            return renderQuadVerticalStepSlabs(dispatcher, level, pos, quadPrediction.slabs(), null, poseStack, vertexConsumer, packedOverlay);
        }

        if (quadPrediction != null && level instanceof Level actualLevel) {
            EnhancedSlabPlacementPredictionCache.clearQuadVerticalStep(actualLevel, pos);
        }
        return renderQuadVerticalStepSlabs(dispatcher, level, pos, savedSlabs, blockEntitySlabs, poseStack, vertexConsumer, packedOverlay);
    }

    public static @Nullable BlockState[] materialSlots(BlockAndTintGetter level, BlockPos pos) {
        Object source = materialSource(level, pos);
        if (source instanceof BlockState[] slots) {
            return slots;
        }
        return source instanceof EnhancedSlabMaterialSavedData.MaterialView view ? view.copySlots() : null;
    }

    public static @Nullable Object materialSource(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        EnhancedSlabMaterialSavedData.MaterialView stored = storedMaterialView(level, pos);
        return stored != null ? stored : EnhancedSlabCreateCompat.getRenderSlots(level, pos);
    }

    private static @Nullable EnhancedSlabMaterialSavedData.MaterialView storedMaterialView(BlockAndTintGetter level, BlockPos pos) {
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

    private static @Nullable Object movingMaterialSlots(Level level, BlockPos renderPos, BlockPos destPos) {
        Object slabs = materialSource(level, renderPos);
        if (slabs != null) {
            return slabs;
        }
        slabs = materialSource(level, destPos);
        if (slabs != null) {
            return slabs;
        }
        for (Direction direction : Direction.values()) {
            slabs = materialSource(level, renderPos.relative(direction));
            if (slabs != null) {
                return slabs;
            }
            slabs = materialSource(level, destPos.relative(direction));
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
        BlockState[] array = slabs instanceof BlockState[] states ? states : null;
        return array != null && index >= 0 && index < array.length ? array[index] : null;
    }

    private static @Nullable BlockState slot(@Nullable Object slabs, int index) {
        return materialSlot(slabs, index);
    }

    private static @Nullable BlockState mergedSlot(@Nullable Object preferred, @Nullable BlockState[] fallback, int index) {
        BlockState slab = slot(preferred, index);
        return slab != null ? slab : slot(fallback, index);
    }

    private static @Nullable EnhancedSlabPistonData pistonDataFromSavedSlots(BlockState movedState, @Nullable Object slabs) {
        if (movedState == null || slabs == null || !hasAnySlot(slabs)) {
            return null;
        }
        if (movedState.getBlock() instanceof VerticalSlabBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_SLAB, new BlockState[]{slot(slabs, 0), slot(slabs, 1)});
        }
        if (movedState.getBlock() instanceof StepBlock && slot(slabs, 0) != null) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.STEP, new BlockState[]{slot(slabs, 0)});
        }
        if (movedState.getBlock() instanceof VerticalStepBlock && slot(slabs, 0) != null) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_STEP, new BlockState[]{slot(slabs, 0)});
        }
        if (movedState.getBlock() instanceof QuadStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_STEP, copySlots(slabs));
        }
        if (movedState.getBlock() instanceof QuadVerticalStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_VERTICAL_STEP, copySlots(slabs));
        }
        return null;
    }

    private static boolean hasAnySlot(@Nullable Object slabs) {
        if (slabs == null) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (slot(slabs, i) != null) {
                return true;
            }
        }
        return false;
    }

    private static BlockState[] copySlots(Object slabs) {
        return new BlockState[]{slot(slabs, 0), slot(slabs, 1), slot(slabs, 2), slot(slabs, 3)};
    }

    private static int countSlabs(@Nullable Object preferred, @Nullable BlockState[] fallback) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (mergedSlot(preferred, fallback, i) != null) {
                count++;
            }
        }
        return count;
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

    private static boolean renderQuadVerticalStepSlabs(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, Object slabs, @Nullable BlockState[] fallback, PoseStack poseStack, VertexConsumer vertexConsumer, int packedOverlay) {
        BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(
                mergedSlot(slabs, fallback, 0), mergedSlot(slabs, fallback, 1),
                mergedSlot(slabs, fallback, 2), mergedSlot(slabs, fallback, 3));
        if (fullBlockState != null) {
            LegacyEnhancedSlabRenderHelper.renderSourceModelToBuffer(dispatcher, level, pos, fullBlockState, poseStack, vertexConsumer, packedOverlay);
            return true;
        }

        boolean rendered = false;
        for (int index = 0; index < 4; index++) {
            BlockState slab = mergedSlot(slabs, fallback, index);
            if (slab == null) {
                continue;
            }

            Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
            Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
            LegacyEnhancedSlabRenderHelper.renderVerticalStepToBuffer(dispatcher, level, pos, slab, facing, side, poseStack, vertexConsumer, packedOverlay);
            rendered = true;
        }
        return rendered;
    }

    private static void renderPistonData(BlockRenderDispatcher dispatcher, BlockAndTintGetter level, BlockPos pos, BlockState movedState, EnhancedSlabPistonData data, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        switch (data.kind()) {
            case VERTICAL_SLAB -> {
                if (!(movedState.getBlock() instanceof VerticalSlabBlock)) return;
                if (movedState.getValue(VerticalSlabBlock.DOUBLE)) {
                    BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(data.slab(0), data.slab(1));
                    if (fullBlockState != null) {
                        LegacyEnhancedSlabRenderHelper.renderSourceModel(dispatcher, level, pos, fullBlockState, poseStack, buffer, packedOverlay);
                        return;
                    }
                }
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
                BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(data.slabs());
                if (fullBlockState != null) {
                    LegacyEnhancedSlabRenderHelper.renderSourceModel(dispatcher, level, pos, fullBlockState, poseStack, buffer, packedOverlay);
                    return;
                }
                Direction.Axis axis = movedState.getValue(QuadStepBlock.AXIS);
                BlockState[] stackedThatch = new BlockState[2];
                for (int bottomIndex = 0; bottomIndex < 2; bottomIndex++) {
                    BlockState thatchFullState = ThatchHalfBlockBakedModel.fullStateForStackedPair(data.slabs(), bottomIndex);
                    if (thatchFullState != null) {
                        stackedThatch[bottomIndex] = thatchFullState;
                        Direction half = axis == Direction.Axis.Z
                                ? (bottomIndex == 0 ? Direction.NORTH : Direction.SOUTH)
                                : (bottomIndex == 0 ? Direction.WEST : Direction.EAST);
                        LegacyEnhancedSlabRenderHelper.renderThatchHalfBlock(
                                dispatcher, level, pos, thatchFullState, half, poseStack, buffer, packedOverlay);
                    }
                }
                for (int index = 0; index < 4; index++) {
                    if (stackedThatch[index & 1] != null) continue;
                    BlockState slab = data.slab(index);
                    if (slab == null) continue;
                    boolean positive = index == 1 || index == 3;
                    Direction facing = axis == Direction.Axis.Z ? (positive ? Direction.SOUTH : Direction.NORTH) : (positive ? Direction.EAST : Direction.WEST);
                    LegacyEnhancedSlabRenderHelper.renderStep(dispatcher, level, pos, slab, facing, index >= 2, poseStack, buffer, 0, packedOverlay);
                }
            }
            case QUAD_VERTICAL_STEP -> {
                if (!(movedState.getBlock() instanceof QuadVerticalStepBlock)) return;
                BlockState fullBlockState = SlabFamilyModelSourceSelector.thatchFullBlockRenderStateForCompleteFamily(data.slabs());
                if (fullBlockState != null) {
                    LegacyEnhancedSlabRenderHelper.renderSourceModel(dispatcher, level, pos, fullBlockState, poseStack, buffer, packedOverlay);
                    return;
                }
                for (int index = 0; index < 4; index++) {
                    BlockState slab = data.slab(index);
                    if (slab == null) continue;
                    Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
                    Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
                    LegacyEnhancedSlabRenderHelper.renderVerticalStep(dispatcher, level, pos, slab, facing, side, poseStack, buffer, 0, packedOverlay);
                }
            }
        }
    }

}
