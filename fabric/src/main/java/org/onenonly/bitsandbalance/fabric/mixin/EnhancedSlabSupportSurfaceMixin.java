package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
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
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Support Surface (Fabric)
 *
 * Makes slabs report sturdy faces so that:
 *   - Top slabs support items hung below (lanterns, chains, hanging signs,
 *     hanging roots, spore blossoms, pointed dripstone, bells, trapdoors, etc.)
 *
 * Targets {@code isFaceSturdy} on {@link BlockBehaviour.BlockStateBase} directly
 * (at RETURN) so the result is evaluated at runtime and NOT frozen by the
 * precomputed {@code BlockStateBase$Cache}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class EnhancedSlabSupportSurfaceMixin {

    @Inject(method = "isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/SupportType;)Z",
            at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$isFaceSturdy(BlockGetter level, BlockPos pos, Direction direction,
                                              SupportType supportType,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (!FabricTweaksConfig.enableEnhancedSlabs) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$supportsStepAttachments(state, level, pos, direction)) {
            cir.setReturnValue(true);
            return;
        }

        if (!(state.getBlock() instanceof SlabBlock)) return;

        SlabType type = state.getValue(SlabBlock.TYPE);

        // Double slabs are already full blocks in vanilla; we don't special-case them here.

        if (type == SlabType.TOP && direction == Direction.DOWN && FabricTweaksConfig.enhancedSlabsHangBelow) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isSignalSource()Z", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$isSignalSource(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsHangBelow) return;

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
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsHangBelow) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$hasPoweredHangingLeverBelow(level, pos, state)) {
            cir.setReturnValue(15);
        }
    }

    @Inject(method = "getDirectSignal(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$getDirectSignal(BlockGetter level, BlockPos pos, Direction direction,
                                                CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) return;
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsHangBelow) return;

        BlockState state = (BlockState) (Object) this;
        if (bitsandbalance$hasPoweredHangingLeverBelow(level, pos, state)) {
            cir.setReturnValue(15);
        }
    }

    private static boolean bitsandbalance$canProxyLeverPower(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.getValue(SlabBlock.TYPE) == SlabType.TOP;
        }
        return FabricTweaksConfig.enhancedSlabsSteps
            && (state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock);
    }

    private static boolean bitsandbalance$hasPoweredHangingLeverBelow(BlockGetter level, BlockPos pos, BlockState state) {
        if (!bitsandbalance$canProxyLeverPower(state)) return false;
        if (level == null || pos == null) return false;

        BlockState belowState = level.getBlockState(pos.below());
        if (!(belowState.getBlock() instanceof LeverBlock)) return false;
        if (!belowState.hasProperty(BlockStateProperties.POWERED) || !belowState.getValue(BlockStateProperties.POWERED)) return false;
        return belowState.hasProperty(FaceAttachedHorizontalDirectionalBlock.FACE)
            && belowState.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) == AttachFace.CEILING;
    }

    private static boolean bitsandbalance$supportsStepAttachments(BlockState state, BlockGetter level,
                                      BlockPos pos, Direction direction) {
        if (!FabricTweaksConfig.enhancedSlabsSteps) return false;

        if (state.getBlock() instanceof StepBlock) {
            Direction facing = state.getValue(StepBlock.FACING);
            boolean top = state.getValue(StepBlock.TOP);
            return direction == facing
                || (direction == Direction.UP && top)
                || (direction == Direction.DOWN && (!top || (top && FabricTweaksConfig.enhancedSlabsHangBelow)));
        }

        if (state.getBlock() instanceof QuadStepBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof QuadStepBlockEntity quad)) return false;

            Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
            return switch (direction) {
            case UP -> quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null;
            case DOWN -> quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_EAST) != null
                || (FabricTweaksConfig.enhancedSlabsHangBelow && (quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null));
            case EAST -> axis == Direction.Axis.X && (quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_EAST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null);
            case WEST -> axis == Direction.Axis.X && (quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null);
            case SOUTH -> axis == Direction.Axis.Z && (quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_EAST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_EAST) != null);
            case NORTH -> axis == Direction.Axis.Z && (quad.getSlabAt(QuadStepBlock.IDX_BOTTOM_WEST) != null
                || quad.getSlabAt(QuadStepBlock.IDX_TOP_WEST) != null);
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
                if (!(be instanceof QuadVerticalStepBlockEntity quad)) return false;

                return switch (direction) {
                case UP, DOWN -> quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_WEST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_EAST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_WEST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_EAST) != null;
                case EAST -> quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_EAST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_EAST) != null;
                case WEST -> quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_WEST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_WEST) != null;
                case SOUTH -> quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_WEST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_SOUTH_EAST) != null;
                case NORTH -> quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_WEST) != null
                    || quad.getSlabAt(QuadVerticalStepBlock.IDX_NORTH_EAST) != null;
                };
            }

        return false;
    }
}
