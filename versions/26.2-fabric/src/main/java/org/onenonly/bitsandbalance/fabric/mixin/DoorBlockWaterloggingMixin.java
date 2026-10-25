package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class DoorBlockWaterloggingMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$prepareLowerHalfWaterlogging(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(AutoWaterloggingRuntime.prepareAddedWaterloggingPlacementState(
                    context.getLevel(),
                    context.getClickedPos(),
                    state
            ));
        }
    }

    @Redirect(
            method = "setPlacedBy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean bitsandbalance$placeUpperHalfWithIndependentWaterlogging(Level level, BlockPos pos, BlockState state) {
        return level.setBlockAndUpdate(pos, AutoWaterloggingRuntime.prepareAddedWaterloggingPlacementState(level, pos, state));
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void bitsandbalance$scheduleWaterTick(BlockState state,
                                                  LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                                  BlockPos pos, Direction direction, BlockPos neighborPos,
                                                  BlockState neighborState, RandomSource random,
                                                  CallbackInfoReturnable<BlockState> cir) {
        if (!AutoWaterloggingRuntime.isAddedWaterloggingActive(state)) {
            return;
        }
        scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$preserveHalfWaterlogging(BlockState state,
                                                         LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                                         BlockPos pos, Direction direction, BlockPos neighborPos,
                                                         BlockState neighborState, RandomSource random,
                                                         CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        cir.setReturnValue(AutoWaterloggingBlockSupport.preserveWaterloggedUpdateShape(state, result));
    }
}
