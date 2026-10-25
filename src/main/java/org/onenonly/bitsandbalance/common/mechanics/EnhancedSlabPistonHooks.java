package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

public final class EnhancedSlabPistonHooks {
    private EnhancedSlabPistonHooks() {
    }

    public static boolean supports(@Nullable BlockState state) {
        if (state == null) return false;
        if (bitsandbalance$isFixedSingle(state)) return false;

        return state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    public static @Nullable EnhancedSlabPistonData capture(Level level, BlockPos sourcePos) {
        BlockState state = level.getBlockState(sourcePos);
        BlockEntity blockEntity = level.getBlockEntity(sourcePos);
        EnhancedSlabPistonData data = EnhancedSlabPistonData.capture(blockEntity);
        if (data != null) {
            return data;
        }
        return bitsandbalance$captureStored(state, EnhancedSlabMaterialSavedData.get(level, sourcePos.asLong()));
    }

    public static void stage(Level level, BlockPos sourcePos, BlockPos destPos, EnhancedSlabPistonData data) {
        if (data == null) return;

        if (level.isClientSide()) {
            EnhancedSlabPistonRenderCache.putMove(sourcePos.asLong(), destPos.asLong(), data);
        } else {
            EnhancedSlabPistonMoveCache.putMove(sourcePos.asLong(), destPos.asLong(), data);
        }
    }

    public static void applyDeferred(Level level, BlockPos pos, BlockState finalState,
                                     @Nullable Direction movementDirection,
                                     @Nullable EnhancedSlabPistonData directData) {
        if (!supports(finalState)) return;

        EnhancedSlabPistonData data = bitsandbalance$resolveDeferredData(level, pos, movementDirection, directData, finalState);
        if (data == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(finalState.getBlock() instanceof EntityBlock)) {
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), data.copySlabs());
            bitsandbalance$clearMovedSource(level, pos, movementDirection);
            if (!level.isClientSide()) {
                level.sendBlockUpdated(pos, finalState, finalState, Block.UPDATE_ALL);
            }
            return;
        }

        if (!bitsandbalance$isCompatible(blockEntity, data.kind())) {
            if (finalState.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity created = entityBlock.newBlockEntity(pos, finalState);
                if (created != null) {
                    created.setLevel(level);
                    level.setBlockEntity(created);
                    blockEntity = created;
                }
            }
        }

        if (!bitsandbalance$apply(blockEntity, data)) {
            return;
        }
        bitsandbalance$clearMovedSource(level, pos, movementDirection);
    }

    private static @Nullable EnhancedSlabPistonData bitsandbalance$captureStored(BlockState state, @Nullable BlockState[] slots) {
        if (state == null || slots == null) {
            return null;
        }
        if (state.getBlock() instanceof VerticalSlabBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_SLAB, slots);
        }
        if (state.getBlock() instanceof StepBlock) {
            BlockState slab = slot(slots, 0);
            return slab == null ? null : new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.STEP, new BlockState[]{slab});
        }
        if (state.getBlock() instanceof VerticalStepBlock) {
            BlockState slab = slot(slots, 0);
            return slab == null ? null : new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.VERTICAL_STEP, new BlockState[]{slab});
        }
        if (state.getBlock() instanceof QuadStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_STEP, slots);
        }
        if (state.getBlock() instanceof QuadVerticalStepBlock) {
            return new EnhancedSlabPistonData(EnhancedSlabPistonData.Kind.QUAD_VERTICAL_STEP, slots);
        }
        return null;
    }

    private static @Nullable EnhancedSlabPistonData bitsandbalance$resolveDeferredData(Level level,
                                                                                        BlockPos pos,
                                                                                        @Nullable Direction movementDirection,
                                                                                        @Nullable EnhancedSlabPistonData directData,
                                                                                        BlockState finalState) {
        if (bitsandbalance$matches(finalState, directData)) {
            return directData;
        }

        long posLong = pos.asLong();
        if (level.isClientSide()) {
            EnhancedSlabPistonRenderCache.Entry entry = bitsandbalance$takeRenderEntry(pos, movementDirection, posLong);
            return entry != null && bitsandbalance$matches(finalState, entry.data()) ? entry.data() : null;
        }

        EnhancedSlabPistonMoveCache.Entry entry = bitsandbalance$takeMoveEntry(pos, movementDirection, posLong);
        return entry != null && bitsandbalance$matches(finalState, entry.data()) ? entry.data() : null;
    }

    private static @Nullable EnhancedSlabPistonRenderCache.Entry bitsandbalance$takeRenderEntry(BlockPos pos,
                                                                                                  @Nullable Direction movementDirection,
                                                                                                  long posLong) {
        if (movementDirection != null) {
            EnhancedSlabPistonRenderCache.Entry entry = EnhancedSlabPistonRenderCache.takeForMove(posLong, pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) return entry;

            entry = EnhancedSlabPistonRenderCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) return entry;
        }
        return EnhancedSlabPistonRenderCache.takeForMove(posLong, posLong);
    }

    private static @Nullable EnhancedSlabPistonMoveCache.Entry bitsandbalance$takeMoveEntry(BlockPos pos,
                                                                                              @Nullable Direction movementDirection,
                                                                                              long posLong) {
        if (movementDirection != null) {
            EnhancedSlabPistonMoveCache.Entry entry = EnhancedSlabPistonMoveCache.takeForMove(posLong, pos.relative(movementDirection.getOpposite()).asLong());
            if (entry != null) return entry;

            entry = EnhancedSlabPistonMoveCache.takeForMove(posLong, pos.relative(movementDirection).asLong());
            if (entry != null) return entry;
        }
        return EnhancedSlabPistonMoveCache.takeForMove(posLong, posLong);
    }

    private static boolean bitsandbalance$matches(BlockState finalState, @Nullable EnhancedSlabPistonData data) {
        if (finalState == null || data == null) return false;
        if (bitsandbalance$isFixedSingle(finalState)) return false;

        return switch (data.kind()) {
            case VERTICAL_SLAB -> finalState.getBlock() instanceof VerticalSlabBlock;
            case STEP -> finalState.getBlock() instanceof StepBlock;
            case VERTICAL_STEP -> finalState.getBlock() instanceof VerticalStepBlock;
            case QUAD_STEP -> finalState.getBlock() instanceof QuadStepBlock;
            case QUAD_VERTICAL_STEP -> finalState.getBlock() instanceof QuadVerticalStepBlock;
        };
    }

    private static boolean bitsandbalance$isCompatible(@Nullable BlockEntity blockEntity, EnhancedSlabPistonData.Kind kind) {
        if (blockEntity == null) return false;

        return switch (kind) {
            case VERTICAL_SLAB -> blockEntity instanceof VerticalSlabBlockEntity;
            case STEP -> blockEntity instanceof StepBlockEntity;
            case VERTICAL_STEP -> blockEntity instanceof VerticalStepBlockEntity;
            case QUAD_STEP -> blockEntity instanceof QuadStepBlockEntity;
            case QUAD_VERTICAL_STEP -> blockEntity instanceof QuadVerticalStepBlockEntity;
        };
    }

    private static boolean bitsandbalance$apply(@Nullable BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (blockEntity == null || data == null) return false;

        return switch (data.kind()) {
            case VERTICAL_SLAB -> bitsandbalance$applyVerticalSlab(blockEntity, data);
            case STEP -> bitsandbalance$applyStep(blockEntity, data);
            case VERTICAL_STEP -> bitsandbalance$applyVerticalStep(blockEntity, data);
            case QUAD_STEP -> bitsandbalance$applyQuadStep(blockEntity, data);
            case QUAD_VERTICAL_STEP -> bitsandbalance$applyQuadVerticalStep(blockEntity, data);
        };
    }

    private static boolean bitsandbalance$applyVerticalSlab(BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (!(blockEntity instanceof VerticalSlabBlockEntity verticalSlab)) return false;

        BlockState facing = data.slab(0);
        BlockState opposite = data.slab(1);
        if (facing == null) return false;
        if (opposite == null) opposite = facing;

        verticalSlab.setSlabs(facing, opposite);
        return true;
    }

    private static boolean bitsandbalance$applyStep(BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (!(blockEntity instanceof StepBlockEntity step)) return false;

        BlockState slab = data.slab(0);
        if (slab == null) return false;

        step.setSlabState(slab);
        return true;
    }

    private static boolean bitsandbalance$applyVerticalStep(BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (!(blockEntity instanceof VerticalStepBlockEntity verticalStep)) return false;

        BlockState slab = data.slab(0);
        if (slab == null) return false;

        verticalStep.setSlabState(slab);
        return true;
    }

    private static boolean bitsandbalance$applyQuadStep(BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (!(blockEntity instanceof QuadStepBlockEntity quadStep)) return false;

        quadStep.setAllSlabs(data.copySlabs());
        return true;
    }

    private static boolean bitsandbalance$applyQuadVerticalStep(BlockEntity blockEntity, EnhancedSlabPistonData data) {
        if (!(blockEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep)) return false;

        quadVerticalStep.setAllSlabs(data.copySlabs());
        return true;
    }

    private static boolean bitsandbalance$isFixedSingle(BlockState state) {
        return state.getBlock() instanceof FixedVerticalSlabBlock
                || state.getBlock() instanceof FixedStepBlock
                || state.getBlock() instanceof FixedVerticalStepBlock;
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static void bitsandbalance$clearMovedSource(Level level, BlockPos finalPos, @Nullable Direction movementDirection) {
        if (level == null || level.isClientSide() || movementDirection == null) {
            return;
        }

        BlockPos sourcePos = finalPos.relative(movementDirection.getOpposite());
        if (!sourcePos.equals(finalPos)) {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong());
        }
    }
}
