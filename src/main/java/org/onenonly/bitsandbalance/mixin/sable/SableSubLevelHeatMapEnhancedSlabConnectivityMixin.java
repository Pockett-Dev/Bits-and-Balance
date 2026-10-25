package org.onenonly.bitsandbalance.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager", remap = false)
public abstract class SableSubLevelHeatMapEnhancedSlabConnectivityMixin {
    @Coerce
    @WrapOperation(
            method = "split()V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper;assembleBlocks(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/lang/Iterable;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)Ldev/ryanhcode/sable/sublevel/ServerSubLevel;"
            ),
            require = 0,
            remap = false
    )
    private Object bitsandbalance$reconcileAssembledEnhancedSlabBodyBeforeValidation(
            ServerLevel level,
            BlockPos origin,
            Iterable<BlockPos> blocks,
            @Coerce Object bounds,
            Operation<Object> original) {
        Object successor = original.call(level, origin, blocks, bounds);
        // Sable validates the returned body's self mass immediately and destroys its whole plot
        // when that mass is still zero. Finalize position-backed Step data on this exact successor
        // before control returns to that validation branch.
        SableCompat.reconcileAssembledEnhancedSlabSubLevelBeforeValidation(successor);
        return successor;
    }

    @Inject(method = "split", at = @At("TAIL"), require = 0, remap = false)
    private void bitsandbalance$reconcileRetainedEnhancedSlabBody(CallbackInfo ci) {
        SableCompat.reconcileRetainedEnhancedSlabSubLevelAfterSplit(this);
    }

    @WrapOperation(
            method = "onSolidAdded",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;heatMapContains(Lnet/minecraft/core/BlockPos;)Z"
            ),
            require = 0
    )
    private boolean bitsandbalance$shapeAwareEnhancedSlabInitialAdd(@Coerce Object heatMapManager,
                                                                     BlockPos neighborPos,
                                                                     Operation<Boolean> original,
                                                                     @Local(argsOnly = true) BlockPos sourcePos) {
        // The intercepted call asks whether neighborPos is already in Sable's heat map. Keep the
        // geometric direction (heated neighbor -> newly added block), but delegate the original
        // query with its real argument. Passing sourcePos here always queried the not-yet-added
        // owner and made a live rebuild behave differently from a fresh load.
        boolean neighborContains = original.call(heatMapManager, neighborPos);
        return SableCompat.isEnhancedSlabHeatMapConnected(
                heatMapManager,
                neighborPos,
                sourcePos,
                neighborContains
        );
    }

    @WrapOperation(
            method = "step",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;isSolidAt(Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 0
            ),
            require = 0
    )
    private boolean bitsandbalance$shapeAwareEnhancedSlabFill(@Coerce Object heatMapManager,
                                                              BlockPos targetPos,
                                                              Operation<Boolean> original,
                                                              @Local(ordinal = 0) BlockPos sourcePos) {
        return bitsandbalance$shapeAwareEnhancedSlabConnection(heatMapManager, sourcePos, targetPos, original);
    }

    @WrapOperation(
            method = "step",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;isSolidAt(Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 1
            ),
            require = 0
    )
    private boolean bitsandbalance$shapeAwareEnhancedSlabClear(@Coerce Object heatMapManager,
                                                               BlockPos targetPos,
                                                               Operation<Boolean> original,
                                                               @Local(ordinal = 0) BlockPos sourcePos) {
        return bitsandbalance$shapeAwareEnhancedSlabConnection(heatMapManager, sourcePos, targetPos, original);
    }

    @WrapOperation(
            method = "step",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;isSolidAt(Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 2
            ),
            require = 0
    )
    private boolean bitsandbalance$shapeAwareEnhancedSlabRemoved(@Coerce Object heatMapManager,
                                                                 BlockPos targetPos,
                                                                 Operation<Boolean> original,
                                                                 @Local(ordinal = 0) BlockPos sourcePos) {
        return bitsandbalance$shapeAwareEnhancedSlabConnection(heatMapManager, sourcePos, targetPos, original);
    }

    @WrapOperation(
            method = "step",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/plot/heat/SubLevelHeatMapManager;isSolidAt(Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 3
            ),
            require = 0
    )
    private boolean bitsandbalance$shapeAwareEnhancedSlabBacktrack(@Coerce Object heatMapManager,
                                                                   BlockPos targetPos,
                                                                   Operation<Boolean> original,
                                                                   @Local(ordinal = 3) BlockPos sourcePos) {
        return bitsandbalance$shapeAwareEnhancedSlabConnection(heatMapManager, sourcePos, targetPos, original);
    }

    private boolean bitsandbalance$shapeAwareEnhancedSlabConnection(Object heatMapManager,
                                                                    BlockPos sourcePos,
                                                                    BlockPos targetPos,
                                                                    Operation<Boolean> original) {
        return SableCompat.isEnhancedSlabHeatMapConnected(
                heatMapManager,
                sourcePos,
                targetPos,
                original.call(heatMapManager, targetPos)
        );
    }
}
