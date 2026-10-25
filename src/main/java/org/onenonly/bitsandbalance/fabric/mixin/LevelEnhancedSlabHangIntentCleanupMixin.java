package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentAccess;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.onenonly.bitsandbalance.fabric.network.FabricEnhancedSlabHangIntentNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelEnhancedSlabHangIntentCleanupMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void bitsandbalance$clearHangIntentWhenBlockChanges(BlockPos pos, BlockState state, int flags,
                                                                CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!((Object) this instanceof ServerLevel level)) return;
        if (EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)) return;

        long posLong = pos.asLong();
        if (EnhancedSlabHangIntentAccess.get(level).remove(posLong)) {
            FabricEnhancedSlabHangIntentNetworking.sendUpdateToAll(level, posLong, false);
        }
    }
}