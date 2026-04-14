package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessMixin extends BitsAndBalanceBlockDisplayAccess {
    @Override
    @Invoker("setBlockState")
    void bitsandbalance$setBlockState(BlockState state);
}
