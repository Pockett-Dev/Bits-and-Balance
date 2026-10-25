package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.client.ThatchHalfBlockBakedModel;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;

public class QuadStepBlockEntityRenderer implements BlockEntityRenderer<QuadStepBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;
    private static final int ALL_DIRECTIONS = LegacyEnhancedSlabRenderHelper.allDirectionMask();

    public QuadStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public void render(QuadStepBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) {
            return;
        }
        if (blockEntity.getLevel() == null) {
            return;
        }
        buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);

        EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (singlePrediction != null) {
            StepBlockEntityRenderer.renderSinglePrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), singlePrediction.stepState(), singlePrediction.slabState(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        EnhancedSlabPlacementPredictionCache.QuadStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        BlockState[] actualSlabs = blockEntity.getAllSlabs();
        if (prediction != null && bitsandbalance$countSlabs(actualSlabs) < 2) {
            StepBlockEntityRenderer.renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), prediction.quadState(), prediction.slabs(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        if (!(blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() instanceof QuadStepBlock)) {
            return;
        }
        if (prediction != null) {
            EnhancedSlabPlacementPredictionCache.clearQuadStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        }

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(actualSlabs);
        if (fullBlockState != null) {
            LegacyEnhancedSlabRenderHelper.renderSourceModel(
                    this.blockRenderDispatcher,
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos(),
                    fullBlockState,
                    poseStack,
                    buffer,
                    packedOverlay
            );
            return;
        }

        Direction.Axis axis = Direction.Axis.X;
        if (blockEntity.getBlockState().getBlock() instanceof QuadStepBlock) {
            axis = blockEntity.getBlockState().getValue(QuadStepBlock.AXIS);
        }

        BlockState[] stackedThatch = new BlockState[2];
        for (int bottomIndex = 0; bottomIndex < 2; bottomIndex++) {
            BlockState thatchFullState = ThatchHalfBlockBakedModel.fullStateForStackedPair(actualSlabs, bottomIndex);
            if (thatchFullState != null) {
                stackedThatch[bottomIndex] = thatchFullState;
                Direction half = axis == Direction.Axis.Z
                        ? (bottomIndex == 0 ? Direction.NORTH : Direction.SOUTH)
                        : (bottomIndex == 0 ? Direction.WEST : Direction.EAST);
                LegacyEnhancedSlabRenderHelper.renderThatchHalfBlock(
                        this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(),
                        thatchFullState, half, poseStack, buffer, packedOverlay);
            }
        }

        BlockState[] slabs = actualSlabs;
        for (int index = 0; index < slabs.length; index++) {
            if (stackedThatch[index & 1] != null) {
                continue;
            }
            BlockState slab = slabs[index];
            if (slab == null) {
                continue;
            }

            boolean positive = index == 1 || index == 3;
            Direction facing = axis == Direction.Axis.Z
                ? (positive ? Direction.SOUTH : Direction.NORTH)
                : (positive ? Direction.EAST : Direction.WEST);
            boolean top = index >= 2;

            LegacyEnhancedSlabRenderHelper.renderStep(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), slab, facing, top, poseStack, buffer, packedLight, packedOverlay, quadStepVisibleMask(axis, index, slabs));
        }
    }

    private static int quadStepVisibleMask(Direction.Axis axis, int index, BlockState[] slabs) {
        int mask = ALL_DIRECTIONS;
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

    private static BlockState slot(BlockState[] slabs, int index) {
        return slabs != null && index >= 0 && index < slabs.length ? slabs[index] : null;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }

    private static int bitsandbalance$countSlabs(BlockState[] slabs) {
        int count = 0;
        for (BlockState slab : slabs) {
            if (slab != null) {
                count++;
            }
        }
        return count;
    }
}
