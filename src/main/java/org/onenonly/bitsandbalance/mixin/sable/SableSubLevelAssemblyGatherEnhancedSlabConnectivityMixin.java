package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SableSubLevelAssemblyGatherEnhancedSlabConnectivityMixin {
    @Inject(
            method = "gatherConnectedBlocks(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;ILdev/ryanhcode/sable/api/SubLevelAssemblyHelper$FrontierPredicate;)Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper$GatherResult;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void bitsandbalance$gatherConnectedEnhancedSlabsByShape(BlockPos gatherOrigin,
                                                                            ServerLevel level,
                                                                            int maximumBlocksToAssemble,
                                                                            @Coerce Object frontierPredicate,
                                                                            CallbackInfoReturnable<Object> cir) {
        Object result = SableCompat.gatherConnectedBlocksShapeAware(gatherOrigin, level, maximumBlocksToAssemble, frontierPredicate);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
