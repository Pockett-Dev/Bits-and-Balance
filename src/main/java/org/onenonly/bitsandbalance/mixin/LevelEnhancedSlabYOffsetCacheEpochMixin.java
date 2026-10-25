package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelEnhancedSlabYOffsetCacheEpochMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void bitsandbalance$bumpEpochOnClientSetBlock(BlockPos pos, BlockState state, int flags,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!((Object) this instanceof ClientLevel)) return;
        if (!cir.getReturnValue()) return;
        EnhancedSlabHelper.invalidateVisualYOffsetCacheAround((Level) (Object) this, pos);
    }
}
