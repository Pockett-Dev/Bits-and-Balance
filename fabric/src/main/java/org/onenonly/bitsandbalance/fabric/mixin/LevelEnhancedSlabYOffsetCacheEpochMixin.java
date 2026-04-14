package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelEnhancedSlabYOffsetCacheEpochMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"),
            require = 0
    )
    private void bitsandbalance$bumpEpochOnClientSetBlock(BlockPos pos, BlockState state, int flags,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!((Object) this instanceof ClientLevel)) return;
        EnhancedSlabHelper.bumpVisualYOffsetCacheEpoch();
    }
}
