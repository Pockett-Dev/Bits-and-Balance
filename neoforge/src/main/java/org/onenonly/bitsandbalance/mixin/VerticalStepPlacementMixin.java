package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
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
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.items.VerticalStepBlockItem;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Step Placement (NeoForge)
 *
 * Intercepts right-click with a vertical step item to:
 * <ol>
 *   <li>Place a normal single-quadrant vertical step block onto empty air/fluid.</li>
 *   <li>Upgrade an existing single vertical step block → {@link QuadVerticalStepBlock} when a
 *       different quadrant is occupied at the same position.</li>
 *   <li>Add a vertical step into an empty quadrant slot of an existing {@link QuadVerticalStepBlock}.</li>
 * </ol>
 */
@Mixin(BlockItem.class)
public abstract class VerticalStepPlacementMixin {

    private static boolean bitsandbalance$isVerticalStepAnchor(BlockState state) {
        return state.getBlock() instanceof VerticalStepBlock || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    private static BlockPos bitsandbalance$adjacentHorizontalTargetVerticalStep(BlockPos clickedPos, Direction clickedFace, Vec3 hit) {
        BlockPos adjacent = clickedPos.relative(clickedFace);
        int y = Mth.floor(hit.y - 1.0E-7);
        return new BlockPos(adjacent.getX(), y, adjacent.getZ());
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$tryVerticalStepPlacement(UseOnContext context,
                                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsSteps) return;

        BlockItem self = (BlockItem) (Object) this;
        Block blockFromItem = self.getBlock();
        Block sourceVerticalSlab = null;
        if (blockFromItem instanceof FixedVerticalStepBlock fixedStep) {
            sourceVerticalSlab = fixedStep.getSourceVerticalSlab();
        } else if (self instanceof VerticalStepBlockItem dynamicItem) {
            sourceVerticalSlab = dynamicItem.getSourceVerticalSlab();
        } else {
            sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(blockFromItem);
        }
        if (sourceVerticalSlab == null) return;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos originalClickedPos = clickedPos;
        Direction clickedFace = context.getClickedFace();
        Vec3 hit = context.getClickLocation();
        double clickY = hit.y;
        Player player = context.getPlayer();

        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedFace.getAxis().isHorizontal() && bitsandbalance$isVerticalStepAnchor(clickedState)) {
            int baseY = clickedPos.getY();
            if (clickY > baseY + 1.001) {
                BlockPos up = clickedPos.above();
                BlockState upState = level.getBlockState(up);
                if (bitsandbalance$isVerticalStepAnchor(upState)) {
                    clickedPos = up;
                    clickedState = upState;
                }
            } else if (clickY < baseY - 0.001) {
                BlockPos down = clickedPos.below();
                BlockState downState = level.getBlockState(down);
                if (bitsandbalance$isVerticalStepAnchor(downState)) {
                    clickedPos = down;
                    clickedState = downState;
                }
            }
        }
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos vanillaPlacePos = placeContext.getClickedPos();

        // Determine whether we are modifying the clicked position or placing next door.
        BlockPos targetPos;
        if (clickedState.getBlock() instanceof VerticalStepBlock && !(clickedState.getBlock() instanceof QuadVerticalStepBlock)) {
            Direction existingFacing = clickedState.getValue(VerticalStepBlock.FACING);
            Direction existingSide = clickedState.getValue(VerticalStepBlock.SIDE);
            boolean sameBlockHorizontal = (clickedFace.getAxis() == Direction.Axis.X && clickedFace == existingSide.getOpposite())
                    || (clickedFace.getAxis() == Direction.Axis.Z && clickedFace == existingFacing.getOpposite());
            if (sameBlockHorizontal) {
                targetPos = clickedPos;
            } else if (clickedFace.getAxis().isHorizontal()) {
                targetPos = bitsandbalance$adjacentHorizontalTargetVerticalStep(clickedPos, clickedFace, hit);
            } else {
                targetPos = vanillaPlacePos;
            }
        } else if (clickedState.getBlock() instanceof QuadVerticalStepBlock) {
            int clickedIndex = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(clickedPos, hit, clickedFace);
            Direction clickedIndexFacing = QuadVerticalStepBlock.facingForIndex(clickedIndex);
            Direction clickedIndexSide = QuadVerticalStepBlock.sideForIndex(clickedIndex);
            boolean sameBlockHorizontal = (clickedFace.getAxis() == Direction.Axis.X && clickedFace == clickedIndexSide.getOpposite())
                    || (clickedFace.getAxis() == Direction.Axis.Z && clickedFace == clickedIndexFacing.getOpposite());
            if (sameBlockHorizontal) {
                targetPos = clickedPos;
            } else if (clickedFace.getAxis().isHorizontal()) {
                targetPos = bitsandbalance$adjacentHorizontalTargetVerticalStep(clickedPos, clickedFace, hit);
            } else {
                targetPos = vanillaPlacePos;
            }
        } else if (clickedState.canBeReplaced(placeContext)) {
            targetPos = clickedPos;
        } else if (clickedFace.getAxis().isHorizontal()) {
            targetPos = bitsandbalance$adjacentHorizontalTargetVerticalStep(clickedPos, clickedFace, hit);
        } else {
            targetPos = vanillaPlacePos;
        }

        BlockState targetState = level.getBlockState(targetPos);

        // Determine quadrant the player is targeting at the target position.
        // Use the actual click location so placement follows the exact corner under the crosshair.
        int targetIndex = VerticalStepBlock.bitsandbalance$getPlacementIndex(targetPos, hit, clickedFace);

        boolean sameBlockInteraction = targetPos.equals(clickedPos)
            && (targetState.getBlock() instanceof VerticalStepBlock
            || targetState.getBlock() instanceof QuadVerticalStepBlock);

        Direction facing = sameBlockInteraction
            ? QuadVerticalStepBlock.facingForIndex(targetIndex)
            : VerticalStepBlock.bitsandbalance$getFacingForPlacement(targetPos, hit, clickedFace);
        Direction side = sameBlockInteraction
            ? QuadVerticalStepBlock.sideForIndex(targetIndex)
            : VerticalStepBlock.bitsandbalance$getSideForPlacement(targetPos, hit, clickedFace);

        // sourceVerticalSlab resolved above

        // ── Case 1: merge with existing single VerticalStepBlock ──────────────
        if (targetState.getBlock() instanceof VerticalStepBlock
                && !(targetState.getBlock() instanceof QuadVerticalStepBlock)) {
            Direction existingFacing = targetState.getValue(VerticalStepBlock.FACING);
            Direction existingSide   = targetState.getValue(VerticalStepBlock.SIDE);
            int existingIndex = QuadVerticalStepBlock.toIndex(
                    existingSide == Direction.EAST, existingFacing == Direction.SOUTH);

            if (existingIndex == targetIndex) {
                return;
            }

            // Read existing slab state from BE
            BlockEntity oldBe = level.getBlockEntity(targetPos);
            BlockState existingSlabState = null;
            if (oldBe instanceof VerticalStepBlockEntity vsbe) {
                existingSlabState = vsbe.getSlabState();
            }
            if (existingSlabState == null) {
                Block existingSourceVSlab = (targetState.getBlock() instanceof FixedVerticalStepBlock fvs)
                        ? fvs.getSourceVerticalSlab()
                        : VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(targetState.getBlock());
                if (existingSourceVSlab != null) {
                    existingSlabState = existingSourceVSlab.defaultBlockState();
                }
            }
            if (existingSlabState == null) return;

            BlockState placing = buildPlacingSlabState(sourceVerticalSlab, context.getItemInHand());

            boolean waterlogged = targetState.getValue(VerticalStepBlock.WATERLOGGED);
            BlockState quadState = CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState()
                    .setValue(QuadVerticalStepBlock.WATERLOGGED, waterlogged);
            if (!level.isClientSide()) {
                level.setBlock(targetPos, quadState, Block.UPDATE_ALL);
                BlockEntity newBe = level.getBlockEntity(targetPos);
                if (newBe instanceof QuadVerticalStepBlockEntity quad) {
                    quad.setSlabAt(existingIndex, existingSlabState);
                    quad.setSlabAt(targetIndex, placing);
                }
                bitsandbalance$verticalStepPlaySound(level, targetPos, placing, targetState);
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // ── Case 2: add to existing QuadVerticalStepBlock ─────────────────────
        if (targetState.getBlock() instanceof QuadVerticalStepBlock) {
            BlockEntity be = level.getBlockEntity(targetPos);
            if (!(be instanceof QuadVerticalStepBlockEntity quad)) return;
            if (quad.getSlabAt(targetIndex) != null) {
                return;
            }

            BlockState placing = buildPlacingSlabState(sourceVerticalSlab, context.getItemInHand());
            if (!level.isClientSide()) {
                quad.setSlabAt(targetIndex, placing);
                level.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                bitsandbalance$verticalStepPlaySound(level, targetPos, placing, targetState);
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // ── Case 3: place fresh VerticalStepBlock ─────────────────────────────
        Block stepForSlab = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(sourceVerticalSlab);
        if (stepForSlab == null) return;

        BlockState newState = stepForSlab.defaultBlockState()
                .setValue(VerticalStepBlock.FACING, facing)
                .setValue(VerticalStepBlock.SIDE, side)
                .setValue(VerticalStepBlock.WATERLOGGED,
                        level.getFluidState(targetPos).getType() == Fluids.WATER);

        if (!level.getBlockState(targetPos).canBeReplaced(placeContext) &&
            !(level.getBlockState(targetPos).isAir())) {
            return;
        }

        if (!level.isClientSide()) {
            level.setBlock(targetPos, newState, Block.UPDATE_ALL);
            BlockState placing = buildPlacingSlabState(sourceVerticalSlab, context.getItemInHand());
            BlockEntity freshBe = level.getBlockEntity(targetPos);
            if (freshBe instanceof VerticalStepBlockEntity vsbe) {
                vsbe.setSlabState(placing);
            }
            bitsandbalance$verticalStepPlaySound(level, targetPos, placing, newState);
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns SOUTH if the hit is on the south (positive-Z) half, otherwise NORTH. */
    @Unique
    private static BlockState buildPlacingSlabState(Block sourceVerticalSlab, ItemStack heldStack) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        BlockState state = sourceSlab != null ? sourceSlab.defaultBlockState() : sourceVerticalSlab.defaultBlockState();

        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }

        return state;
    }

    @Unique
    private static Direction bitsandbalance$verticalStepFacingFromHit(BlockPos pos, Vec3 hit, Direction clickedFace) {
        double rz = hit.z - pos.getZ();
        return rz > 0.5 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Returns EAST if the hit is on the east (positive-X) half, otherwise WEST. */
    @Unique
    private static Direction bitsandbalance$verticalStepSideFromHit(BlockPos pos, Vec3 hit, Direction clickedFace) {
        double rx = hit.x - pos.getX();
        return rx > 0.5 ? Direction.EAST : Direction.WEST;
    }

    @Unique
    private static void bitsandbalance$verticalStepPlaySound(Level level, BlockPos pos, BlockState preferredState, BlockState fallbackState) {
        try {
            BlockState soundState = preferredState != null ? preferredState : fallbackState;
            if (soundState == null) {
                return;
            }
            SoundType sound = soundState.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(),
                    SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        } catch (Throwable ignored) {
        }
    }
}
