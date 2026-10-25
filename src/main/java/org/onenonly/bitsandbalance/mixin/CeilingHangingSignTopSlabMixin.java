package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CeilingHangingSignBlock.class)
public abstract class CeilingHangingSignTopSlabMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$allowCeilingSignsUnderEnhancedSlabs(BlockState state, LevelReader level,
                                                                    BlockPos pos,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;

        BlockPos supportPos = pos.above();
        BlockState supportState = level.getBlockState(supportPos);
        if (EnhancedSlabHelper.hasFullBottomSurface(level, supportPos, supportState)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
        method = "getStateForPlacement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;isFaceFull(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/Direction;)Z"
        )
    )
    private boolean bitsandbalance$treatTopSlabsAsFullCeilings(
            VoxelShape shape, Direction direction, BlockPlaceContext context) {

        if (Block.isFaceFull(shape, direction)) {
            return true;
        }
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow || direction != Direction.DOWN) {
            return false;
        }

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (EnhancedSlabHelper.hasFullBottomSurface(level, clickedPos, clickedState)) {
            return true;
        }

        BlockPos abovePos = context.getClickedPos().above();
        BlockState aboveState = level.getBlockState(abovePos);
        return EnhancedSlabHelper.hasFullBottomSurface(level, abovePos, aboveState);
    }
}