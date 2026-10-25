package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.config.EnhancedSlabFeatureFlags;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Side-click Adjacent Placement
 *
 * When a block is visually shifted by the slab-grid offset system, its outline
 * shape can extend into neighboring Y cell(s). Vanilla placement always anchors
 * side-placement to the clicked block's integer position, which causes blocks
 * placed on the side of the visually-shifted portion to land at the wrong Y.
 *
 * This mixin adjusts the final placement position returned by
 * {@link BlockPlaceContext#getClickedPos()} so that its Y matches the voxel cell
 * containing the actual click point.
 */
@Mixin(BlockPlaceContext.class)
public abstract class EnhancedSlabBlockPlaceContextGridMixin {

    @Inject(method = "getClickedPos", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$alignPlacementPosToSlabGrid(CallbackInfoReturnable<BlockPos> cir) {
        if (!EnhancedSlabFeatureFlags.enabled()) return;

        BlockPlaceContext self = (BlockPlaceContext) (Object) this;
        BlockHitResult hit = ((UseOnContextAccessor) self).bitsandbalance$getHitResult();
        BlockPos anchorPos = hit.getBlockPos();
        Direction face = self.getClickedFace();
        if (face.getAxis() == Direction.Axis.Y) {
            bitsandbalance$routeSlabPlacementPastStepBundle(self, anchorPos, face, cir);
            return;
        }

        if (bitsandbalance$routeTopTrapdoorPlacementBelowHalfBlockCeiling(self, anchorPos, face, cir)) {
            return;
        }

        if (bitsandbalance$preserveVanillaClickedPos(self)) return;

        Level level = self.getLevel();
        BlockState anchorState = level.getBlockState(anchorPos);
        if (anchorState.isAir()) return;
        if (EnhancedSlabHelper.shouldExcludeFromVisualOffset(anchorState)) return;

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, anchorPos, anchorState);
        if (yOff == 0.0) return;

        double clickY = self.getClickLocation().y;
        int clickCellY = Mth.floor(clickY - 1.0E-7);
        int deltaY = clickCellY - anchorPos.getY();
        if (deltaY == 0) return;

        BlockPos placePos = cir.getReturnValue();
        cir.setReturnValue(placePos.offset(0, deltaY, 0));
    }

    private static boolean bitsandbalance$routeTopTrapdoorPlacementBelowHalfBlockCeiling(BlockPlaceContext context,
                                                                                         BlockPos anchorPos,
                                                                                         Direction face,
                                                                                         CallbackInfoReturnable<BlockPos> cir) {
        if (!face.getAxis().isHorizontal()) return false;
        if (!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;
        if (!EnhancedSlabHelper.isTrapdoorLike(blockItem.getBlock().defaultBlockState())) return false;

        Level level = context.getLevel();
        BlockPos adjacentSupportPos = anchorPos.relative(face);
        BlockState adjacentSupportState = level.getBlockState(adjacentSupportPos);
        if (EnhancedSlabHelper.hasHangBelowOffsetSurface(level, adjacentSupportPos, adjacentSupportState)
                && bitsandbalance$isClickInBottomSlabSlot(context, anchorPos)) {
            BlockPos routedPos = adjacentSupportPos.below();
            if (!level.getBlockState(routedPos).canBeReplaced()) return false;

            cir.setReturnValue(routedPos);
            return true;
        }

        return false;
    }

    private static boolean bitsandbalance$isClickInBottomSlabSlot(BlockPlaceContext context, BlockPos pos) {
        double localY = context.getClickLocation().y - pos.getY();
        return localY >= -1.0E-6D && localY < 0.5D + 1.0E-6D;
    }

    private static void bitsandbalance$routeSlabPlacementPastStepBundle(BlockPlaceContext context,
                                                                        BlockPos anchorPos,
                                                                        Direction face,
                                                                        CallbackInfoReturnable<BlockPos> cir) {
        if (!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof SlabBlock)) return;

        BlockState anchorState = context.getLevel().getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof StepBlock) && !(anchorState.getBlock() instanceof QuadStepBlock)) {
            //? if fabric && mc == 1.21.1
            if (face == Direction.DOWN
                    && !(anchorState.getBlock() instanceof SlabBlock)
                    && EnhancedSlabHelper.hasHangBelowOffsetSurface(context.getLevel(), anchorPos, anchorState)) {
                cir.setReturnValue(anchorPos.relative(face));
            }
            //? endif
            return;
        }

        BlockPos placePos = cir.getReturnValue();
        if (!placePos.equals(anchorPos)) return;

        cir.setReturnValue(anchorPos.relative(face));
    }

    private static boolean bitsandbalance$preserveVanillaClickedPos(BlockPlaceContext context) {
        if (!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;
        return EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(blockItem.getBlock().defaultBlockState());
    }
}
