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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — Mixed Double Slab Placement (Fabric)
 *
 * Intercepts {@link BlockItem#useOn} at HEAD. When the player right-clicks a
 * single slab with a <em>different</em> slab type in a configuration that would
 * form a double slab, this mixin creates a {@link MixedSlabBlock} instead of
 * letting vanilla fail and place at the adjacent position.
 */
@Mixin(BlockItem.class)
public abstract class MixedSlabPlacementMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$tryMixedSlabPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;

        // Is the held item a slab block item?
        BlockItem self = (BlockItem) (Object) this;
        Block placingBlock = self.getBlock();
        if (!(placingBlock instanceof SlabBlock)) return;

        if (bitsandbalance$tryPlacePastStepBundle(self, context, cir, placingBlock)) {
            return;
        }

        if (!FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs) return;
        if (CommonBlocks.MIXED_SLAB == null) return;

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        double clickY = context.getClickLocation().y;

        if (bitsandbalance$tryCreateMixedAt(context, cir, placingBlock, clickedPos, clickedFace, clickY - clickedPos.getY())) {
            return;
        }

        BlockPos adjacentPos = clickedPos.relative(clickedFace);
        if (!adjacentPos.equals(clickedPos)) {
            bitsandbalance$tryCreateMixedAt(context, cir, placingBlock, adjacentPos, clickedFace.getOpposite(), clickY - adjacentPos.getY());
        }
    }

    private static boolean bitsandbalance$tryPlacePastStepBundle(BlockItem self,
                                                                 UseOnContext context,
                                                                 CallbackInfoReturnable<InteractionResult> cir,
                                                                 Block placingBlock) {
        Direction clickedFace = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!(clickedState.getBlock() instanceof StepBlock) && !(clickedState.getBlock() instanceof QuadStepBlock)) {
            return false;
        }

        BlockPos targetPos;
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            targetPos = clickedPos.relative(clickedFace);
        } else {
            int clickCellY = Mth.floor(context.getClickLocation().y - 1.0E-7D);
            int deltaY = clickCellY - clickedPos.getY();
            if (deltaY == 0) {
                return false;
            }
            targetPos = clickedPos.offset(0, deltaY, 0);
        }

        if (targetPos.equals(clickedPos)) {
            return false;
        }

        BlockPlaceContext redirectedContext = BlockPlaceContext.at(new BlockPlaceContext(context), targetPos, clickedFace);
        InteractionResult redirectedResult = self.place(redirectedContext);
        if (!redirectedResult.consumesAction()) {
            return false;
        }

        cir.setReturnValue(redirectedResult);
        return true;
    }

    private static boolean bitsandbalance$tryCreateMixedAt(UseOnContext context,
                                                            CallbackInfoReturnable<InteractionResult> cir,
                                                            Block placingBlock,
                                                            BlockPos targetPos,
                                                            Direction targetFace,
                                                            double hitY) {
        Level level = context.getLevel();
        BlockState existingState = level.getBlockState(targetPos);
        if (!(existingState.getBlock() instanceof SlabBlock)) return false;

        // Derive the actual placing state including palette tint from the held item.
        BlockState placingState = bitsandbalance$coloredPlacingState(placingBlock, context.getItemInHand());

        boolean canCreate = MixedSlabBlock.canCreateMixedSlab(existingState, placingBlock, targetFace, hitY);

        if (!canCreate) return false;

        BlockState[] halves = MixedSlabBlock.buildMixedSlabStates(existingState, placingState);
        BlockState bottomSlab = halves[0];
        BlockState topSlab = halves[1];

        boolean waterlogged = existingState.hasProperty(SlabBlock.WATERLOGGED)
                && existingState.getValue(SlabBlock.WATERLOGGED);
        BlockState mixedState = CommonBlocks.MIXED_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged);

        level.setBlock(targetPos, mixedState, Block.UPDATE_ALL);

        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof MixedSlabBlockEntity mixedBe) {
            mixedBe.setSlabs(bottomSlab, topSlab);
        }

        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }

        SoundType soundType = placingState.getSoundType();
        level.playSound(null, targetPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);

        if (waterlogged) {
            level.scheduleTick(targetPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
        return true;
    }

    private static BlockState bitsandbalance$coloredPlacingState(Block placingBlock, ItemStack heldStack) {
        BlockState state = placingBlock.defaultBlockState();
        return state;
    }
}
