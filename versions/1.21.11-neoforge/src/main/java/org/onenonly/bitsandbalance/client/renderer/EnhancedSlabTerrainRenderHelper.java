package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;

import java.util.List;

public final class EnhancedSlabTerrainRenderHelper {
    private static final ThreadLocal<Integer> NESTED_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);

    private EnhancedSlabTerrainRenderHelper() {
    }

    public static boolean isEnabled() {
        return Config.enableEnhancedSlabs;
    }

    public static boolean handles(BlockState state) {
        if (!isEnabled() || state == null) {
            return false;
        }

        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    public static boolean render(BlockRenderDispatcher dispatcher,
                                 BlockAndTintGetter level,
                                 BlockPos pos,
                                 BlockState state,
                                 PoseStack poseStack,
                                 VertexConsumer consumer) {
        if (dispatcher == null || level == null || pos == null || poseStack == null || consumer == null) {
            return false;
        }
        if (NESTED_RENDER_DEPTH.get() > 0 || !handles(state)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState[] materialSlots = EnhancedSlabCreateCompat.getRenderSlots(level, pos);
        EnhancedSlabMaterialSavedData.MaterialView materialView = materialSlots == null ? storedMaterialView(level, pos) : null;
        if (state.getBlock() instanceof MixedSlabBlock) {
            BlockState bottomSlab = slot(materialSlots, materialView, 0);
            BlockState topSlab = slot(materialSlots, materialView, 1);
            if (bottomSlab == null && topSlab == null) {
                return false;
            }
            MixedSlabRenderHelper.renderTerrain(dispatcher, level, pos, bottomSlab, topSlab, poseStack, consumer);
            return true;
        }

        if (state.getBlock() instanceof VerticalSlabBlock) {
            BlockState facingSlab = blockEntity instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab.getFacingSlab() : null;
            BlockState oppositeSlab = blockEntity instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab.getOppositeSlab() : null;
            facingSlab = firstPresent(slot(materialSlots, materialView, 0), facingSlab);
            oppositeSlab = firstPresent(slot(materialSlots, materialView, 1), oppositeSlab);
            if (state.getValue(VerticalSlabBlock.DOUBLE)) {
                BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(facingSlab, oppositeSlab);
                if (renderFullBlock(dispatcher, level, pos, fullBlockState, poseStack, consumer)) {
                    return true;
                }
            }
            boolean rendered = false;
            if (facingSlab != null) {
                VerticalSlabBlockEntityRenderer.renderTerrainHalf(dispatcher, level, pos, facingSlab, state.getValue(VerticalSlabBlock.FACING), poseStack, consumer);
                rendered = true;
            }
            if (state.getValue(VerticalSlabBlock.DOUBLE) && oppositeSlab != null) {
                VerticalSlabBlockEntityRenderer.renderTerrainHalf(dispatcher, level, pos, oppositeSlab, state.getValue(VerticalSlabBlock.FACING).getOpposite(), poseStack, consumer);
                rendered = true;
            }
            return rendered;
        }

        if (state.getBlock() instanceof StepBlock) {
            BlockState slab = blockEntity instanceof StepBlockEntity step ? step.getSlabState() : null;
            slab = firstPresent(slot(materialSlots, materialView, 0), slab);
            if (slab == null) {
                return false;
            }
            StepBlockEntityRenderer.renderTerrainQuadrant(dispatcher, level, pos, slab, state.getValue(StepBlock.FACING), state.getValue(StepBlock.TOP), poseStack, consumer);
            return true;
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            BlockState slab = blockEntity instanceof VerticalStepBlockEntity verticalStep ? verticalStep.getSlabState() : null;
            slab = firstPresent(slot(materialSlots, materialView, 0), slab);
            if (slab == null) {
                return false;
            }
            VerticalStepBlockEntityRenderer.renderTerrainQuadrant(dispatcher, level, pos, slab, state.getValue(VerticalStepBlock.FACING), state.getValue(VerticalStepBlock.SIDE), poseStack, consumer);
            return true;
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            QuadStepBlockEntity quadStep = blockEntity instanceof QuadStepBlockEntity quad ? quad : null;
            BlockState s0 = firstPresent(slot(materialSlots, materialView, 0), quadStep == null ? null : quadStep.getSlabAt(0));
            BlockState s1 = firstPresent(slot(materialSlots, materialView, 1), quadStep == null ? null : quadStep.getSlabAt(1));
            BlockState s2 = firstPresent(slot(materialSlots, materialView, 2), quadStep == null ? null : quadStep.getSlabAt(2));
            BlockState s3 = firstPresent(slot(materialSlots, materialView, 3), quadStep == null ? null : quadStep.getSlabAt(3));
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(s0, s1, s2, s3);
            if (renderFullBlock(dispatcher, level, pos, fullBlockState, poseStack, consumer)) {
                return true;
            }
            boolean rendered = false;
            for (int i = 0; i < 4; i++) {
                BlockState slab = indexedSlot(s0, s1, s2, s3, i);
                if (slab == null) {
                    continue;
                }

                boolean positive = i == 1 || i == 3;
                Direction facing = axis == Direction.Axis.Z
                        ? (positive ? Direction.SOUTH : Direction.NORTH)
                        : (positive ? Direction.EAST : Direction.WEST);
                boolean top = i >= 2;
                StepBlockEntityRenderer.renderTerrainQuadrant(dispatcher, level, pos, slab, facing, top, poseStack, consumer);
                rendered = true;
            }
            return rendered;
        }

        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            QuadVerticalStepBlockEntity quadVertical = blockEntity instanceof QuadVerticalStepBlockEntity quad ? quad : null;
            BlockState s0 = firstPresent(slot(materialSlots, materialView, 0), quadVertical == null ? null : quadVertical.getSlabAt(0));
            BlockState s1 = firstPresent(slot(materialSlots, materialView, 1), quadVertical == null ? null : quadVertical.getSlabAt(1));
            BlockState s2 = firstPresent(slot(materialSlots, materialView, 2), quadVertical == null ? null : quadVertical.getSlabAt(2));
            BlockState s3 = firstPresent(slot(materialSlots, materialView, 3), quadVertical == null ? null : quadVertical.getSlabAt(3));
            BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(s0, s1, s2, s3);
            if (renderFullBlock(dispatcher, level, pos, fullBlockState, poseStack, consumer)) {
                return true;
            }
            boolean rendered = false;
            for (int i = 0; i < 4; i++) {
                BlockState slab = indexedSlot(s0, s1, s2, s3, i);
                if (slab == null) {
                    continue;
                }

                Direction facing = (i == 2 || i == 3) ? Direction.SOUTH : Direction.NORTH;
                Direction side = (i == 1 || i == 3) ? Direction.EAST : Direction.WEST;
                VerticalStepBlockEntityRenderer.renderTerrainQuadrant(dispatcher, level, pos, slab, facing, side, poseStack, consumer);
                rendered = true;
            }
            return rendered;
        }

        return false;
    }

    private static boolean renderFullBlock(BlockRenderDispatcher dispatcher,
                                           BlockAndTintGetter level,
                                           BlockPos pos,
                                           @Nullable BlockState fullBlockState,
                                           PoseStack poseStack,
                                           VertexConsumer consumer) {
        if (fullBlockState == null) {
            return false;
        }
        List<BlockModelPart> parts = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(fullBlockState)
                .collectParts(RandomSource.create(fullBlockState.getSeed(pos)));
        if (parts == null || parts.isEmpty()) {
            return false;
        }
        renderNested(dispatcher, fullBlockState, pos, level, poseStack, consumer, parts);
        return true;
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
        return slots != null && index >= 0 && index < slots.length ? slots[index] : view == null ? null : view.slot(index);
    }

    private static @Nullable BlockState indexedSlot(BlockState s0, BlockState s1, BlockState s2, BlockState s3, int index) {
        return switch (index) {
            case 0 -> s0;
            case 1 -> s1;
            case 2 -> s2;
            case 3 -> s3;
            default -> null;
        };
    }

    private static @Nullable BlockState firstPresent(@Nullable BlockState primary, @Nullable BlockState fallback) {
        return primary != null ? primary : fallback;
    }

    static void renderNested(BlockRenderDispatcher dispatcher,
                             BlockState renderState,
                             BlockPos pos,
                             BlockAndTintGetter level,
                             PoseStack poseStack,
                             VertexConsumer consumer,
                             List<BlockModelPart> parts) {
        if (dispatcher == null || renderState == null || pos == null || level == null || poseStack == null || consumer == null || parts == null || parts.isEmpty()) {
            return;
        }

        int previousDepth = NESTED_RENDER_DEPTH.get();
        NESTED_RENDER_DEPTH.set(previousDepth + 1);
        try {
            dispatcher.renderBatched(renderState, pos, level, poseStack, consumer, false, parts);
        } finally {
            NESTED_RENDER_DEPTH.set(previousDepth);
        }
    }
}
