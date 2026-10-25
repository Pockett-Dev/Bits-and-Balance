package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FluidState;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AutoWaterloggingFluidStateMixin {
    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$addedWaterloggingFluidState(CallbackInfoReturnable<FluidState> cir) {
        BlockState state = (BlockState) (Object) this;
        if (AutoWaterloggingRuntime.isAddedWaterloggingActive(state)) {
            cir.setReturnValue(AutoWaterloggingBlockSupport.getFluidState(state));
        }
    }
}
