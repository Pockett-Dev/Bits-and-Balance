package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DoorBlock.class, FenceGateBlock.class, PressurePlateBlock.class, WeightedPressurePlateBlock.class})
public abstract class AutoWaterloggingBlockStateMixin implements SimpleWaterloggedBlock {
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void bitsandbalance$addWaterloggedProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        AutoWaterloggingBlockSupport.addWaterloggedProperty(builder);
    }
}
