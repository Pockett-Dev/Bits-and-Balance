package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Step Placement (NeoForge)
 *
 * Intercepts right-click with a step item to:
 * <ol>
 *   <li>Place a normal single-quadrant step block onto empty air/fluid.</li>
 *   <li>Upgrade an existing single step block → {@link QuadStepBlock} when a
 *       different quadrant is occupied at the same position.</li>
 *   <li>Add a step into an empty quadrant slot of an existing {@link QuadStepBlock}.</li>
 * </ol>
 */
@Mixin(BlockItem.class)
public abstract class StepPlacementMixin {

    private static boolean bitsandbalance$isStepAnchor(BlockState state) {
        return state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock;
    }

    private static BlockPos bitsandbalance$adjacentHorizontalTarget(BlockPos clickedPos, Direction clickedFace, Vec3 hit) {
        BlockPos adjacent = clickedPos.relative(clickedFace);
        int y = Mth.floor(hit.y - 1.0E-7);
        return new BlockPos(adjacent.getX(), y, adjacent.getZ());
    }

    private static boolean bitsandbalance$positiveSideForAxis(Direction.Axis axis, BlockPos pos, Vec3 hit, Direction clickedFace) {
        if (axis == Direction.Axis.Z) {
            if (clickedFace == Direction.SOUTH) return true;
            if (clickedFace == Direction.NORTH) return false;
            return (hit.z - pos.getZ()) > 0.5;
        }
        // X axis
        if (clickedFace == Direction.EAST) return true;
        if (clickedFace == Direction.WEST) return false;
        return (hit.x - pos.getX()) > 0.5;
    }

    private static boolean bitsandbalance$positiveSideForPlacement(Direction.Axis axis, BlockPos clickedPos, BlockPos targetPos, Vec3 hit, Direction clickedFace) {
        if (!targetPos.equals(clickedPos) && clickedFace.getAxis() == axis) {
            return StepBlock.bitsandbalance$isPositiveFacing(clickedFace.getOpposite());
        }
        return bitsandbalance$positiveSideForAxis(axis, targetPos, hit, clickedFace);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$tryStepPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsSteps) return;

        BlockItem self = (BlockItem) (Object) this;
        Block blockFromItem = self.getBlock();
        if (!(blockFromItem instanceof FixedStepBlock fixedStep)) return;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos originalClickedPos = clickedPos;
        Direction clickedFace = context.getClickedFace();
        Vec3 hit = context.getClickLocation();
        double clickY = hit.y;
        Player player = context.getPlayer();

        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedFace.getAxis().isHorizontal() && bitsandbalance$isStepAnchor(clickedState)) {
            int baseY = clickedPos.getY();
            if (clickY > baseY + 1.001) {
                BlockPos up = clickedPos.above();
                BlockState upState = level.getBlockState(up);
                if (bitsandbalance$isStepAnchor(upState)) {
                    clickedPos = up;
                    clickedState = upState;
                }
            } else if (clickY < baseY - 0.001) {
                BlockPos down = clickedPos.below();
                BlockState downState = level.getBlockState(down);
                if (bitsandbalance$isStepAnchor(downState)) {
                    clickedPos = down;
                    clickedState = downState;
                }
            }
        }
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos vanillaPlacePos = placeContext.getClickedPos();

        // Determine whether we are modifying the clicked position or placing next door.
        BlockPos targetPos;
        if (clickedState.getBlock() instanceof StepBlock && !(clickedState.getBlock() instanceof QuadStepBlock)) {
            Direction existingFacing = clickedState.getValue(StepBlock.FACING);
            boolean existingTop = clickedState.getValue(StepBlock.TOP);
            Direction.Axis existingAxis = existingFacing.getAxis();
            boolean existingPositive = StepBlock.bitsandbalance$isPositiveFacing(existingFacing);
            int existingIndex = QuadStepBlock.toIndex(existingPositive, existingTop);
            int preferredIndex = QuadStepBlock.toIndex(
                    bitsandbalance$positiveSideForAxis(existingAxis, clickedPos, hit, clickedFace),
                    StepBlock.bitsandbalance$getTopForPlacement(clickedPos, hit, clickedFace));
            boolean sameBlockHorizontal = clickedFace.getAxis().isHorizontal()
                && clickedFace == existingFacing.getOpposite();
            boolean sameBlockVertical = (clickedFace == Direction.UP && !existingTop)
                || (clickedFace == Direction.DOWN && existingTop);
            boolean sameBlockFill = clickedFace.getAxis().isHorizontal() && preferredIndex != existingIndex;
            if (sameBlockHorizontal || sameBlockVertical || sameBlockFill) {
                targetPos = clickedPos;
            } else if (clickedFace.getAxis().isHorizontal()) {
                targetPos = bitsandbalance$adjacentHorizontalTarget(clickedPos, clickedFace, hit);
            } else {
                targetPos = vanillaPlacePos;
            }
        } else if (clickedState.getBlock() instanceof QuadStepBlock) {
            Direction.Axis axis = clickedState.getValue(QuadStepBlock.AXIS);
            int clickedIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(clickedPos, hit, clickedFace, axis);
            Direction clickedIndexSide = QuadStepBlock.sideForIndex(axis, clickedIndex);
            boolean sameBlockHorizontal = clickedFace.getAxis().isHorizontal()
                && clickedFace == clickedIndexSide.getOpposite();
            boolean sameBlockVertical = false;
            BlockEntity clickedBe = level.getBlockEntity(clickedPos);
            if (clickedBe instanceof QuadStepBlockEntity quad) {
            int sideBottomIndex = QuadStepBlock.toIndex(QuadStepBlock.isPositive(clickedIndex), false);
            int sideTopIndex = QuadStepBlock.toIndex(QuadStepBlock.isPositive(clickedIndex), true);
            sameBlockVertical = (clickedFace == Direction.UP
                && quad.getSlabAt(sideBottomIndex) != null
                && quad.getSlabAt(sideTopIndex) == null)
                || (clickedFace == Direction.DOWN
                && quad.getSlabAt(sideTopIndex) != null
                && quad.getSlabAt(sideBottomIndex) == null);
            int preferredIndex = QuadStepBlock.toIndex(
                    bitsandbalance$positiveSideForAxis(axis, clickedPos, hit, clickedFace),
                    StepBlock.bitsandbalance$getTopForPlacement(clickedPos, hit, clickedFace));
            boolean sameBlockFill = clickedFace.getAxis().isHorizontal()
                && preferredIndex >= 0
                && preferredIndex < 4
                && quad.getSlabAt(preferredIndex) == null;
            if (sameBlockHorizontal || sameBlockVertical || sameBlockFill) {
                targetPos = clickedPos;
            } else if (clickedFace.getAxis().isHorizontal()) {
                targetPos = bitsandbalance$adjacentHorizontalTarget(clickedPos, clickedFace, hit);
            } else {
                targetPos = vanillaPlacePos;
            }
            } else if (sameBlockHorizontal || sameBlockVertical) {
                targetPos = clickedPos;
            } else if (clickedFace.getAxis().isHorizontal()) {
                targetPos = bitsandbalance$adjacentHorizontalTarget(clickedPos, clickedFace, hit);
            } else {
                targetPos = vanillaPlacePos;
            }
        } else if (clickedState.canBeReplaced(placeContext)) {
            targetPos = clickedPos;
        } else if (clickedFace.getAxis().isHorizontal()) {
            targetPos = bitsandbalance$adjacentHorizontalTarget(clickedPos, clickedFace, hit);
        } else {
            targetPos = vanillaPlacePos;
        }

        if (clickedFace == Direction.DOWN && targetPos.equals(clickedPos) && !vanillaPlacePos.equals(clickedPos)) {
            BlockState vanillaTargetState = level.getBlockState(vanillaPlacePos);
            if (bitsandbalance$isStepAnchor(vanillaTargetState)) {
                targetPos = vanillaPlacePos;
            }
        }

        BlockState targetState = level.getBlockState(targetPos);

        Block slabBlock = fixedStep.getSourceSlab();

        // ── Case 1: merge with existing single StepBlock ──────────────────────
        if (targetState.getBlock() instanceof StepBlock && !(targetState.getBlock() instanceof QuadStepBlock)) {
            Direction existingFacing = targetState.getValue(StepBlock.FACING);
            boolean existingTop = targetState.getValue(StepBlock.TOP);
            Direction.Axis axis = existingFacing.getAxis();
            boolean existingPositive = StepBlock.bitsandbalance$isPositiveFacing(existingFacing);
            int existingIndex = QuadStepBlock.toIndex(existingPositive, existingTop);

            boolean positiveSide = bitsandbalance$positiveSideForPlacement(axis, clickedPos, targetPos, hit, clickedFace);
            boolean wantTop = StepBlock.bitsandbalance$getTopForPlacement(targetPos, hit, clickedFace);
            int bottomIndex = QuadStepBlock.toIndex(positiveSide, false);
            int topIndex = QuadStepBlock.toIndex(positiveSide, true);
            if (clickedFace == Direction.UP) {
                // Stacking only: placing onto an existing bottom step makes a top step.
                wantTop = (existingIndex == bottomIndex);
            } else if (clickedFace == Direction.DOWN) {
                // Clicking the underside of an occupied top step should fill the lower slot.
                wantTop = existingIndex != topIndex;
            }
            int targetIndex = QuadStepBlock.toIndex(positiveSide, wantTop);
            if (targetPos.equals(clickedPos) && clickedFace.getAxis().isHorizontal() && wantTop != existingTop) {
                targetIndex = QuadStepBlock.toIndex(existingPositive, wantTop);
            }

            if (existingIndex == targetIndex) {
                return;
            }

            // Upgrade to QuadStepBlock
            BlockEntity oldBe = level.getBlockEntity(targetPos);
            BlockState existingSlabState = null;
            if (oldBe instanceof StepBlockEntity sbe) {
                existingSlabState = sbe.getSlabState();
            }
            if (existingSlabState == null) {
                Block existingSourceSlab = (targetState.getBlock() instanceof FixedStepBlock fs)
                        ? fs.getSourceSlab() : StepDynamicRegistry.getSlabForStep(targetState.getBlock());
                if (existingSourceSlab != null) {
                    existingSlabState = existingSourceSlab.defaultBlockState();
                }
            }
            if (existingSlabState == null) return;

            BlockState placing = buildPlacingSlabState(slabBlock, context);

            boolean waterlogged = targetState.getValue(StepBlock.WATERLOGGED);
            BlockState quadState = CommonBlocks.QUAD_STEP.defaultBlockState()
                    .setValue(QuadStepBlock.WATERLOGGED, waterlogged)
                    .setValue(QuadStepBlock.AXIS, axis);
            if (!level.isClientSide()) {
                level.setBlock(targetPos, quadState, Block.UPDATE_ALL);
                BlockEntity newBe = level.getBlockEntity(targetPos);
                if (newBe instanceof QuadStepBlockEntity quad) {
                    quad.setSlabAt(existingIndex, existingSlabState);
                    quad.setSlabAt(targetIndex, placing);
                }
                bitsandbalance$playSound(level, targetPos, placing, targetState);
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // ── Case 2: add to existing QuadStepBlock ─────────────────────────────
        if (targetState.getBlock() instanceof QuadStepBlock) {
            BlockEntity be = level.getBlockEntity(targetPos);
            if (!(be instanceof QuadStepBlockEntity quad)) return;
            Direction.Axis axis = targetState.getValue(QuadStepBlock.AXIS);
            boolean positiveSide = bitsandbalance$positiveSideForPlacement(axis, clickedPos, targetPos, hit, clickedFace);

            int bottomIdx = QuadStepBlock.toIndex(positiveSide, false);
            int topIdx = QuadStepBlock.toIndex(positiveSide, true);
            boolean wantTop = StepBlock.bitsandbalance$getTopForPlacement(targetPos, hit, clickedFace);
            if (clickedFace == Direction.UP && quad.getSlabAt(bottomIdx) != null && quad.getSlabAt(topIdx) == null) {
                wantTop = true;
            } else if (clickedFace == Direction.DOWN && quad.getSlabAt(topIdx) != null && quad.getSlabAt(bottomIdx) == null) {
                wantTop = false;
            }
            int targetIndex = QuadStepBlock.toIndex(positiveSide, wantTop);
            if (player != null && clickedFace.getAxis().isHorizontal() && targetPos.equals(clickedPos)) {
                int targetedIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(targetPos, hit, clickedFace, axis);
                if (targetedIndex >= 0 && targetedIndex < 4 && quad.getSlabAt(targetedIndex) != null
                        && wantTop != QuadStepBlock.isTop(targetedIndex)) {
                    targetIndex = QuadStepBlock.toIndex(QuadStepBlock.isPositive(targetedIndex), wantTop);
                }
            }
            if (quad.getSlabAt(targetIndex) != null) {
                return;
            }

            BlockState placing = buildPlacingSlabState(slabBlock, context);
            if (!level.isClientSide()) {
                quad.setSlabAt(targetIndex, placing);
                // Sync the block state to client
                level.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                bitsandbalance$playSound(level, targetPos, placing, targetState);
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // ── Case 3: place fresh StepBlock (let vanilla do it, but ensure BE is set) ─
        // Find the correct step block to place
        Block stepForSlab = StepDynamicRegistry.getStepForSlab(slabBlock);
        if (stepForSlab == null) return;

        // Fresh placement should feel "normal": pin half by clicked face when possible,
        // otherwise choose the closest horizontal side; always places on the bottom layer.
        boolean flushAdjacentHorizontalFromStep = clickedFace.getAxis().isHorizontal()
            && !targetPos.equals(clickedPos)
            && (clickedState.getBlock() instanceof StepBlock || clickedState.getBlock() instanceof QuadStepBlock);
        Direction placementFacing = flushAdjacentHorizontalFromStep
            ? clickedFace.getOpposite()
            : StepBlock.bitsandbalance$getFacingForPlacement(targetPos, hit, clickedFace, player);
        boolean placementTop = StepBlock.bitsandbalance$getTopForPlacement(targetPos, hit, clickedFace);

        BlockState newState = stepForSlab.defaultBlockState()
                .setValue(StepBlock.FACING, placementFacing)
                .setValue(StepBlock.TOP, placementTop)
                .setValue(StepBlock.WATERLOGGED,
                        level.getFluidState(targetPos).getType() == Fluids.WATER);

        if (!level.getBlockState(targetPos).canBeReplaced(placeContext) &&
            !(level.getBlockState(targetPos).isAir())) {
            return;
        }

        if (level.isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        level.setBlock(targetPos, newState, Block.UPDATE_ALL);
        BlockEntity freshBe = level.getBlockEntity(targetPos);
        if (freshBe instanceof StepBlockEntity sbe) {
            BlockState placing = buildPlacingSlabState(slabBlock, context);
            sbe.setSlabState(placing);
        }
        bitsandbalance$playSound(level, targetPos, newState, newState);
        context.getItemInHand().shrink(1);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static BlockState buildPlacingSlabState(Block slabBlock, UseOnContext context) {
        BlockState state = slabBlock.defaultBlockState();

        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }

        return state;
    }

    private static void bitsandbalance$playSound(Level level, BlockPos pos, BlockState preferredState, BlockState fallbackState) {
        try {
            BlockState soundState = preferredState != null ? preferredState : fallbackState;
            SoundType sound = soundState.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(),
                    SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        } catch (Throwable ignored) {
        }
    }

}
