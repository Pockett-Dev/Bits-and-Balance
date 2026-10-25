package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class AutoWaterloggingDefaultStateMixin {
    @Inject(method = "defaultBlockState", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$addedWaterloggingBlocksDefaultDry(CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null
                && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)
                && AutoWaterloggingBlockSupport.isAddedWaterloggingBlockFamily((Block) (Object) this)) {
            cir.setReturnValue(state.setValue(BlockStateProperties.WATERLOGGED, false));
        }
    }
}
