package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
//? if mc != 1.21.1
import net.minecraft.util.RandomSource;
//? endif
//? if mc == 1.21.1
import net.minecraft.world.level.LevelAccessor;
//? else
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//? endif
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockWaterloggingMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$prepareFenceGateWaterlogging(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(AutoWaterloggingRuntime.prepareAddedWaterloggingPlacementState(
                    context.getLevel(),
                    context.getClickedPos(),
                    state
            ));
        }
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void bitsandbalance$scheduleWaterTick(BlockState state,
//? if mc == 1.21.1
                                                  Direction direction, BlockState neighborState, LevelAccessor level,
                                                  BlockPos pos, BlockPos neighborPos,
//? else
                                                  LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                                  BlockPos pos, Direction direction, BlockPos neighborPos,
                                                  BlockState neighborState, RandomSource random,
//? endif
                                                  CallbackInfoReturnable<BlockState> cir) {
        if (!AutoWaterloggingRuntime.isAddedWaterloggingActive(state)) {
            return;
        }
//? if mc == 1.21.1
        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
//? else
        scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
//? endif
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$preserveWaterlogging(BlockState state,
//? if mc == 1.21.1
                                                     Direction direction, BlockState neighborState, LevelAccessor level,
                                                     BlockPos pos, BlockPos neighborPos,
//? else
                                                     LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                                     BlockState neighborState, RandomSource random,
//? endif
                                                     CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        cir.setReturnValue(AutoWaterloggingBlockSupport.preserveWaterloggedUpdateShape(state, result));
    }
}
