package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.inventory.FabricAutomaticBlockRestock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Vertical Slab Placement (Fabric)
 *
 * Placing a vertical slab item places/merges vertical slabs.
 *
 * Note: This intentionally does NOT support the legacy "shift + slab" placement
 * workflow — vertical slabs have their own items now.
 */
@Mixin(BlockItem.class)
public abstract class VerticalSlabPlacementMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$tryVerticalSlabPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsVerticalSlabs) return;

        BlockItem self = (BlockItem) (Object) this;
        Block blockFromItem = self.getBlock();
        if (!(blockFromItem instanceof FixedVerticalSlabBlock verticalSlabBlock)) return;

        Direction clickedFace = context.getClickedFace();
        Block placingBlock = verticalSlabBlock.getSourceSlab();

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        double clickY = context.getClickLocation().y;

        BlockPos placePos;
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() instanceof VerticalSlabBlock) {
            int baseY = clickedPos.getY();
            if (clickY > baseY + 1.001) {
                BlockPos up = clickedPos.above();
                BlockState upState = level.getBlockState(up);
                if (upState.getBlock() instanceof VerticalSlabBlock) {
                    clickedPos = up;
                    clickedState = upState;
                }
            } else if (clickY < baseY - 0.001) {
                BlockPos down = clickedPos.below();
                BlockState downState = level.getBlockState(down);
                if (downState.getBlock() instanceof VerticalSlabBlock) {
                    clickedPos = down;
                    clickedState = downState;
                }
            }
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos logicalPlacePos = bitsandbalance$getLogicalPlacePos(
            placeContext.getClickedPos(), clickedPos, clickedFace, context.getClickLocation());
        Direction sameBlockMergeFacing = bitsandbalance$getSameBlockMergeFacing(
            clickedState, clickedFace, clickedPos, context.getClickLocation());
        Direction preferredMergeFacing = bitsandbalance$getAdjacentMergeFacing(
            level, clickedState, placeContext, clickedPos, clickedFace, logicalPlacePos);
        Direction gapFillFacing = sameBlockMergeFacing == null && preferredMergeFacing == null
            ? bitsandbalance$getAdjacentGapFillFacing(level, placeContext, clickedPos, clickedFace, logicalPlacePos, context.getClickLocation())
            : null;
        // Vanilla slabs are "replaceable" for merging. Our vertical slab block isn't, so we
        // explicitly allow targeting the existing block when we're trying to fill the empty half.
        if (sameBlockMergeFacing != null) {
            placePos = clickedPos;
        } else if (preferredMergeFacing != null) {
            placePos = logicalPlacePos;
        } else if (gapFillFacing != null) {
            placePos = logicalPlacePos;
        } else if (clickedState.getBlock() instanceof VerticalSlabBlock
                && !clickedState.getValue(VerticalSlabBlock.DOUBLE)
                && clickedFace == clickedState.getValue(VerticalSlabBlock.FACING).getOpposite()) {
            placePos = clickedPos;
        } else if (clickedState.getBlock() instanceof VerticalSlabBlock && clickedFace.getAxis() == Direction.Axis.Y) {
            placePos = clickedPos.relative(clickedFace);
        } else if (clickedState.canBeReplaced(placeContext)) {
            placePos = clickedPos;
        } else {
            placePos = logicalPlacePos;
        }

        Direction facing;
        if (sameBlockMergeFacing != null) {
            facing = sameBlockMergeFacing;
        } else if (preferredMergeFacing != null) {
            facing = preferredMergeFacing;
        } else if (gapFillFacing != null) {
            facing = gapFillFacing;
        } else if (placePos.equals(clickedPos) && clickedFace.getAxis().isHorizontal()) {
            facing = clickedFace;
        } else {
            facing = VerticalSlabBlock.bitsandbalance$getFacingForPlacement(
                    placePos, context.getClickLocation(), clickedFace);
        }
        facing = bitsandbalance$relaxFacingForExistingMerge(level, placePos, facing);

        bitsandbalance$tryPlaceOrMerge(level, context, cir, placingBlock, verticalSlabBlock, placePos, facing);
    }

    private static Direction bitsandbalance$relaxFacingForExistingMerge(Level level, BlockPos placePos, Direction facing) {
        BlockState targetState = level.getBlockState(placePos);
        if (targetState.getBlock() instanceof VerticalSlabBlock && !targetState.getValue(VerticalSlabBlock.DOUBLE)) {
            return targetState.getValue(VerticalSlabBlock.FACING).getOpposite();
        }
        return facing;
    }

    private static Direction bitsandbalance$getSameBlockMergeFacing(BlockState clickedState,
                                                                    Direction clickedFace,
                                                                    BlockPos clickedPos,
                                                                    Vec3 hit) {
        if (!(clickedState.getBlock() instanceof VerticalSlabBlock) || clickedState.getValue(VerticalSlabBlock.DOUBLE)) {
            return null;
        }

        Direction existingFacing = clickedState.getValue(VerticalSlabBlock.FACING);
        Direction mergeFacing = existingFacing.getOpposite();
        if (clickedFace == mergeFacing) {
            return mergeFacing;
        }

        if (clickedFace.getAxis() == Direction.Axis.Y
            && VerticalSlabBlock.bitsandbalance$getTargetedHalf(clickedPos, hit, existingFacing) == mergeFacing) {
            return mergeFacing;
        }

        return null;
    }

    private static BlockPos bitsandbalance$getLogicalPlacePos(BlockPos vanillaPlacePos,
                                                              BlockPos clickedPos,
                                                              Direction clickedFace,
                                                              Vec3 hit) {
        if (!clickedFace.getAxis().isHorizontal()) {
            return vanillaPlacePos;
        }

        int hitCellY = Mth.floor(hit.y - 1.0E-7D);
        if (hitCellY == clickedPos.getY()) {
            return vanillaPlacePos;
        }

        BlockPos adjacent = clickedPos.relative(clickedFace);
        return new BlockPos(adjacent.getX(), hitCellY, adjacent.getZ());
    }

    private static Direction bitsandbalance$getAdjacentMergeFacing(Level level,
                                                                   BlockState clickedState,
                                                                   BlockPlaceContext placeContext,
                                                                   BlockPos clickedPos,
                                                                   Direction clickedFace,
                                                                   BlockPos logicalPlacePos) {
        if (!clickedFace.getAxis().isHorizontal() || logicalPlacePos.equals(clickedPos)) {
            return null;
        }

        BlockState logicalState = level.getBlockState(logicalPlacePos);
        if (!(logicalState.getBlock() instanceof VerticalSlabBlock) || logicalState.getValue(VerticalSlabBlock.DOUBLE)) {
            return null;
        }

        Direction existingFacing = logicalState.getValue(VerticalSlabBlock.FACING);
        return existingFacing.getOpposite();
    }

    private static Direction bitsandbalance$getAdjacentGapFillFacing(Level level,
                                                                     BlockPlaceContext placeContext,
                                                                     BlockPos clickedPos,
                                                                     Direction clickedFace,
                                                                     BlockPos logicalPlacePos,
                                                                     Vec3 hit) {
        if (logicalPlacePos == null) {
            return null;
        }

        BlockState targetState = level.getBlockState(logicalPlacePos);
        if (!targetState.canBeReplaced(placeContext) && !targetState.isAir() && targetState.getFluidState().getType() != Fluids.WATER) {
            return null;
        }

        Direction naturalFacing = VerticalSlabBlock.bitsandbalance$getFacingForPlacement(logicalPlacePos, hit, clickedFace);
        for (Direction candidate : bitsandbalance$gapFillCandidateOrder(logicalPlacePos, clickedFace, hit, naturalFacing)) {
            if (bitsandbalance$canFillAgainstNeighbor(level, logicalPlacePos, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Direction[] bitsandbalance$gapFillCandidateOrder(BlockPos pos,
                                                                    Direction clickedFace,
                                                                    Vec3 hit,
                                                                    Direction naturalFacing) {
        if (!clickedFace.getAxis().isHorizontal()) {
            if (naturalFacing.getAxis() == Direction.Axis.X) {
                return new Direction[]{naturalFacing, naturalFacing.getOpposite(), Direction.NORTH, Direction.SOUTH};
            }
            return new Direction[]{naturalFacing, naturalFacing.getOpposite(), Direction.EAST, Direction.WEST};
        }

        Direction left = bitsandbalance$leftDirection(clickedFace);
        Direction right = bitsandbalance$rightDirection(clickedFace);
        Direction nearEdge = bitsandbalance$leftToRightRatio(pos, clickedFace, hit) < 0.5D ? left : right;
        Direction farEdge = nearEdge == left ? right : left;

        if (naturalFacing == left || naturalFacing == right) {
            Direction otherEdge = naturalFacing == left ? right : left;
            return new Direction[]{naturalFacing, otherEdge, clickedFace.getOpposite(), clickedFace};
        }
        return new Direction[]{nearEdge, farEdge, naturalFacing, clickedFace};
    }

    private static boolean bitsandbalance$canFillAgainstNeighbor(Level level, BlockPos targetPos, Direction candidateFacing) {
        BlockState neighbor = level.getBlockState(targetPos.relative(candidateFacing));
        return neighbor.getBlock() instanceof VerticalSlabBlock
            && !neighbor.getValue(VerticalSlabBlock.DOUBLE)
            && neighbor.getValue(VerticalSlabBlock.FACING) == candidateFacing.getOpposite();
    }

    private static double bitsandbalance$leftToRightRatio(BlockPos pos, Direction clickedFace, Vec3 hit) {
        double rx = Mth.clamp(hit.x - pos.getX(), 0.0D, 1.0D);
        double rz = Mth.clamp(hit.z - pos.getZ(), 0.0D, 1.0D);

        return switch (clickedFace) {
            case NORTH -> 1.0D - rx;
            case SOUTH -> rx;
            case EAST -> 1.0D - rz;
            case WEST -> rz;
            default -> 0.5D;
        };
    }

    private static Direction bitsandbalance$leftDirection(Direction clickedFace) {
        return switch (clickedFace) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.WEST;
        };
    }

    private static Direction bitsandbalance$rightDirection(Direction clickedFace) {
        return switch (clickedFace) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.EAST;
        };
    }

    private static boolean bitsandbalance$tryPlaceOrMerge(Level level,
                                                         UseOnContext context,
                                                         CallbackInfoReturnable<InteractionResult> cir,
                                                         Block placingBlock,
                                                         VerticalSlabBlock verticalSlabBlock,
                                                         BlockPos pos,
                                                         Direction facing) {
        BlockState placingState = bitsandbalance$verticalColoredPlacingState(placingBlock, context.getItemInHand());
        BlockState existing = level.getBlockState(pos);

        if (existing.getBlock() instanceof VerticalSlabBlock && !existing.getValue(VerticalSlabBlock.DOUBLE)) {
            Direction existingFacing = existing.getValue(VerticalSlabBlock.FACING);
            if (facing != existingFacing.getOpposite()) return false;

            BlockEntity be = level.getBlockEntity(pos);
            BlockState facingSlab = null;
            if (be instanceof VerticalSlabBlockEntity vbe) {
                facingSlab = vbe.getFacingSlab();
            }
            if (facingSlab == null) {
                BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                if (stored != null && stored.length > 0) {
                    facingSlab = stored[0];
                }
            }
            if (facingSlab == null && existing.getBlock() instanceof FixedVerticalSlabBlock fixed) {
                facingSlab = fixed.getSourceSlab().defaultBlockState();
            }
            if (facingSlab == null) {
                Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(existing.getBlock());
                if (sourceSlab != null) {
                    facingSlab = sourceSlab.defaultBlockState();
                }
            }
            if (facingSlab == null) return false;

                VerticalSlabBlock targetBlock = (existing.getBlock() instanceof FixedVerticalSlabBlock fixed
                    && fixed.getSourceSlab() != placingBlock)
                    ? CommonBlocks.VERTICAL_SLAB
                    : (VerticalSlabBlock) existing.getBlock();

                boolean waterlogged = existing.hasProperty(VerticalSlabBlock.WATERLOGGED)
                    && existing.getValue(VerticalSlabBlock.WATERLOGGED);

                BlockState newState = targetBlock.defaultBlockState()
                    .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged)
                    .setValue(VerticalSlabBlock.FACING, existingFacing)
                    .setValue(VerticalSlabBlock.DOUBLE, true);

                level.setBlock(pos, newState, Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), facingSlab, placingState);
            if (level.isClientSide()) {
                level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_ALL);
            }

            BlockEntity be2 = level.getBlockEntity(pos);
            if (be2 instanceof VerticalSlabBlockEntity vbe2) {
                vbe2.setSlabs(facingSlab, placingState);
            }
            EnhancedSlabStorageOptimizer.optimizeAt(level, pos);

            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                FabricAutomaticBlockRestock.shrinkAndRestock(context);
            }

            SoundType soundType = placingBlock.defaultBlockState().getSoundType();
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);

            cir.setReturnValue(InteractionResult.SUCCESS);
            return true;
        }

        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!existing.canBeReplaced(placeContext) && !existing.isAir() && existing.getFluidState().getType() != Fluids.WATER) {
            return false;
        }

        boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
        BlockState verticalState = verticalSlabBlock.defaultBlockState()
                .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged)
                .setValue(VerticalSlabBlock.FACING, facing)
                .setValue(VerticalSlabBlock.DOUBLE, false);

        level.setBlock(pos, verticalState, Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), placingState);
        if (level.isClientSide()) {
            level.sendBlockUpdated(pos, verticalState, verticalState, Block.UPDATE_ALL);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            vbe.setSingleFacingSlab(placingState);
        }

        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            FabricAutomaticBlockRestock.shrinkAndRestock(context);
        }

        SoundType soundType = placingBlock.defaultBlockState().getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);

        if (waterlogged) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
        return true;
    }

    private static BlockState bitsandbalance$verticalColoredPlacingState(Block placingBlock, ItemStack heldStack) {
        BlockState state = placingBlock.defaultBlockState();
        return state;
    }

}
