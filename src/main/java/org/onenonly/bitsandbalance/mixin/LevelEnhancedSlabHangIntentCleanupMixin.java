package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentAccess;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.network.EnhancedSlabHangIntentSync;
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
        if (!Config.enableEnhancedSlabs) return;
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!((Object) this instanceof ServerLevel level)) return;
        if (EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)) return;

        long posLong = pos.asLong();
        if (EnhancedSlabHangIntentAccess.get(level).remove(posLong)) {
            EnhancedSlabHangIntentSync.sendUpdateToAll(level, posLong, false);
        }
    }
}