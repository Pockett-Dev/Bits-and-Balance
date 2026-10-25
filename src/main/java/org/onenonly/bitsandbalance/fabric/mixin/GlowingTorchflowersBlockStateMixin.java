package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class GlowingTorchflowersBlockStateMixin {
    private static final int TORCHFLOWER_LIGHT_LEVEL = 8;

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$makeTorchflowersGlow(CallbackInfoReturnable<Integer> cir) {
        if (!FabricTweaksConfig.enableGlowingTorchflowers) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() == Blocks.TORCHFLOWER || state.getBlock() == Blocks.POTTED_TORCHFLOWER) {
            cir.setReturnValue(TORCHFLOWER_LIGHT_LEVEL);
        }
    }
}
