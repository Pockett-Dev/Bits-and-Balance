package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
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
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
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
        if (!FabricTweaksConfig.isVerticalSlabRuntimeEnabled()) return;

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
        BlockPos vanillaPlacePos = placeContext.getClickedPos();
        // Vanilla slabs are "replaceable" for merging. Our vertical slab block isn't, so we
        // explicitly allow targeting the existing block when we're trying to fill the empty half.
        if (clickedState.getBlock() instanceof VerticalSlabBlock
                && !clickedState.getValue(VerticalSlabBlock.DOUBLE)
                && clickedFace == clickedState.getValue(VerticalSlabBlock.FACING).getOpposite()) {
            placePos = clickedPos;
        } else if (clickedState.getBlock() instanceof VerticalSlabBlock && clickedFace.getAxis() == Direction.Axis.Y) {
            placePos = clickedPos.relative(clickedFace);
        } else if (clickedState.canBeReplaced(placeContext)) {
            placePos = clickedPos;
        } else {
            placePos = vanillaPlacePos;
        }

        Direction facing;
        if (placePos.equals(clickedPos) && clickedFace.getAxis().isHorizontal()) {
            facing = clickedFace;
        } else {
            facing = VerticalSlabBlock.bitsandbalance$getFacingForPlacement(
                    placePos, context.getClickLocation(), clickedFace);
        }

        bitsandbalance$tryPlaceOrMerge(level, context, cir, placingBlock, verticalSlabBlock, placePos, facing);
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
            if (!(be instanceof VerticalSlabBlockEntity vbe)) return false;
            BlockState facingSlab = vbe.getFacingSlab();
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

            BlockEntity be2 = level.getBlockEntity(pos);
            if (be2 instanceof VerticalSlabBlockEntity vbe2) {
                vbe2.setSlabs(facingSlab, placingState);
            }

            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
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

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VerticalSlabBlockEntity vbe) {
            vbe.setSingleFacingSlab(placingState);
        }

        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
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
