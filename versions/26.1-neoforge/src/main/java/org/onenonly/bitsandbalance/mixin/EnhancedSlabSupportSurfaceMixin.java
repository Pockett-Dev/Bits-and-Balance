package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Support Surface
 *
 * Keeps the physical step/quad-step support faces that enhanced slabs expose.
 * Ceiling-only underside support for specific blocks is handled by targeted
 * placement/survival mixins instead of a blanket sturdy-face override.
 *
 * Targets {@code isFaceSturdy} on {@link BlockBehaviour.BlockStateBase} directly
 * (at RETURN) so the result is evaluated at runtime and NOT frozen by the
 * precomputed {@code BlockStateBase$Cache}. This allows config toggling without
 * a game restart.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class EnhancedSlabSupportSurfaceMixin {

    /**
     * After the vanilla/cached isFaceSturdy result is computed, override it
     * for slab blocks based on the slab type and the queried direction.
     *
    * <ul>
    *   <li>Top slab    → report sturdy DOWN  (hang items below)</li>
    * </ul>
     *
     */
    @Inject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z",
            at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$isFaceSturdy(BlockGetter level, BlockPos pos, Direction direction,
                                              SupportType supportType,
                                              CallbackInfoReturnable<Boolean> cir) {
        BlockState renderState = (BlockState) (Object) this;
        if (supportType == SupportType.FULL && !(level instanceof Level)
                && (renderState.getBlock() instanceof StepBlock
                || renderState.getBlock() instanceof VerticalStepBlock
                || renderState.getBlock() instanceof VerticalSlabBlock
                || renderState.getBlock() instanceof QuadStepBlock
                || renderState.getBlock() instanceof QuadVerticalStepBlock)) {
            // Render snapshots need real face coverage. Attachment allowances on a live level
            // must not make a quarter-block look like a full supporting face to terrain models.
            cir.setReturnValue(org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderFaceCoverage
                    .isFull(renderState, level, pos, direction));
            return;
        }
        // Fast exit if already sturdy — nothing to override
        if (cir.getReturnValue()) return;

        if (!Config.enableEnhancedSlabs) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$supportsVerticalSlabAttachments(state, direction)) {
            cir.setReturnValue(true);
            return;
        }

        if (bitsandbalance$supportsStepAttachments(state, level, pos, direction)) {
            cir.setReturnValue(true);
            return;
        }

        if (!(state.getBlock() instanceof SlabBlock)) return;

        SlabType type = state.getValue(SlabBlock.TYPE);

        // Double slabs are already full blocks in vanilla; we don't special-case them here.

    }

    private static boolean bitsandbalance$supportsVerticalSlabAttachments(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof VerticalSlabBlock)) return false;
        if (state.getValue(VerticalSlabBlock.DOUBLE)) return false;
        return direction == Direction.UP;
    }

    @Inject(method = "isSignalSource()Z", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$isSignalSource(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$canProxyLeverPower(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$getSignal(BlockGetter level, BlockPos pos, Direction direction,
                                          CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$hasPoweredHangingAttachmentBelow(level, pos, state)) {
            cir.setReturnValue(15);
        }
    }

    @Inject(method = "getDirectSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$getDirectSignal(BlockGetter level, BlockPos pos, Direction direction,
                                                CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$hasPoweredHangingAttachmentBelow(level, pos, state)) {
            cir.setReturnValue(15);
        }
    }

    private static boolean bitsandbalance$canProxyLeverPower(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.getValue(SlabBlock.TYPE) == SlabType.TOP;
        }
        return Config.enhancedSlabsSteps
            && (state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock);
    }

    private static boolean bitsandbalance$hasPoweredHangingAttachmentBelow(BlockGetter level, BlockPos pos, BlockState state) {
        if (!bitsandbalance$canProxyLeverPower(state)) return false;
        if (level == null || pos == null) return false;

        BlockState belowState = level.getBlockState(pos.below());
        if (!(belowState.getBlock() instanceof LeverBlock) && !(belowState.getBlock() instanceof ButtonBlock)) return false;
        if (!belowState.hasProperty(BlockStateProperties.POWERED) || !belowState.getValue(BlockStateProperties.POWERED)) return false;
        return belowState.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)
            && belowState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) == AttachFace.CEILING;
    }

    private static boolean bitsandbalance$supportsStepAttachments(BlockState state, BlockGetter level,
                                      BlockPos pos, Direction direction) {
        if (!Config.enhancedSlabsSteps) return false;

        if (state.getBlock() instanceof StepBlock) {
            Direction facing = state.getValue(StepBlock.FACING);
            boolean top = state.getValue(StepBlock.TOP);
            return direction == facing
                || (direction == Direction.UP && top)
                || (direction == Direction.DOWN && !top);
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
            BlockState[] stored = quad == null ? bitsandbalance$storedSlabs(level, pos, state) : null;
            if (quad == null && stored == null) return false;

            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            return switch (direction) {
            case UP -> bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_WEST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_EAST);
            case DOWN -> bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_WEST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_EAST)
                || (Config.enhancedSlabsHangBelow && (bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_WEST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_EAST)));
            case EAST -> axis == Direction.Axis.X && (bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_EAST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_EAST));
            case WEST -> axis == Direction.Axis.X && (bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_WEST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_WEST));
            case SOUTH -> axis == Direction.Axis.Z && (bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_EAST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_EAST));
            case NORTH -> axis == Direction.Axis.Z && (bitsandbalance$has(quad, stored, QuadStepBlock.IDX_BOTTOM_WEST)
                || bitsandbalance$has(quad, stored, QuadStepBlock.IDX_TOP_WEST));
            };
        }

            if (state.getBlock() instanceof VerticalStepBlock) {
                Direction facing = state.getValue(VerticalStepBlock.FACING);
                Direction side = state.getValue(VerticalStepBlock.SIDE);
                return direction == facing
                    || direction == side
                    || direction == Direction.UP
                    || direction == Direction.DOWN;
            }

            if (state.getBlock() instanceof QuadVerticalStepBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                QuadVerticalStepBlockEntity quad = be instanceof QuadVerticalStepBlockEntity q ? q : null;
                BlockState[] stored = quad == null ? bitsandbalance$storedSlabs(level, pos, state) : null;
                if (quad == null && stored == null) return false;

                return switch (direction) {
                case UP, DOWN -> bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_WEST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_EAST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_WEST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_EAST);
                case EAST -> bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_EAST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_EAST);
                case WEST -> bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_WEST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_WEST);
                case SOUTH -> bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_WEST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_SOUTH_EAST);
                case NORTH -> bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_WEST)
                    || bitsandbalance$has(quad, stored, QuadVerticalStepBlock.IDX_NORTH_EAST);
                };
            }

        return false;
    }

    private static BlockState[] bitsandbalance$storedSlabs(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState[] createSlots = EnhancedSlabCreateCompat.getContraptionSlots(level, pos, state);
        if (createSlots != null) return createSlots;
        return level instanceof Level actualLevel ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong()) : null;
    }

    private static boolean bitsandbalance$has(QuadStepBlockEntity quad, BlockState[] stored, int index) {
        return quad != null ? quad.getSlabAt(index) != null : bitsandbalance$slot(stored, index) != null;
    }

    private static boolean bitsandbalance$has(QuadVerticalStepBlockEntity quad, BlockState[] stored, int index) {
        return quad != null ? quad.getSlabAt(index) != null : bitsandbalance$slot(stored, index) != null;
    }

    private static BlockState bitsandbalance$slot(BlockState[] stored, int index) {
        return stored != null && index >= 0 && index < stored.length ? stored[index] : null;
    }
}
