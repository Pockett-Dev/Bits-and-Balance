package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class AutoWaterloggingBlockPlacementMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$prepareAddedFamilyDefaultPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily((Block) (Object) this)) {
            cir.setReturnValue(AutoWaterloggingRuntime.prepareAddedWaterloggingPlacementState(
                    context.getLevel(),
                    context.getClickedPos(),
                    state
            ));
        }
    }
}
