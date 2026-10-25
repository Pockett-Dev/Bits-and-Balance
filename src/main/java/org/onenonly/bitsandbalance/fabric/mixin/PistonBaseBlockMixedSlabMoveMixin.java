package org.onenonly.bitsandbalance.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.onenonly.bitsandbalance.fabric.compat.FabricCarpetPistonCompat;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricPistonMoveCaptureContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows pistons to move B&B's composite slabs and configured block entities.
 *
 * <p>The hooks deliberately avoid redirecting either {@code hasBlockEntity}
 * or moving-piston construction. Carpet 1.4.147 redirects both operations and
 * treats a missed redirect as fatal. The expression modifier composes with
 * Carpet's pushability result, while movement data is captured after the real
 * piston resolver succeeds and attached by {@link MovingPistonBlockCaptureMixin}
 * when the actual moving block entity is created.</p>
 */
@Mixin(value = PistonBaseBlock.class, priority = 1100)
@SuppressWarnings({"null", "UnusedMethod"})
public abstract class PistonBaseBlockMixedSlabMoveMixin {

    @ModifyExpressionValue(
            method = "isPushable",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z")
    )
    private static boolean bitsandbalance$allowConfiguredBlockEntitiesToMove(
            boolean original,
            @Local(argsOnly = true) BlockState state) {
        if (state != null && state.getBlock() instanceof MixedSlabBlock) {
            return false;
        }
        if (EnhancedSlabPistonHooks.supports(state)) {
            return false;
        }

        // Carpet owns generic block-entity movement while its rule is enabled.
        // Keeping its result also preserves Carpet's unsafe-block exclusions.
        if (FabricCarpetPistonCompat.ownsGenericBlockEntityMovement()) {
            return original;
        }
        if (PistonBlockEntityMoveRuntime.enabled && PistonBlockEntityMoveHooks.supports(state)) {
            return false;
        }
        return original;
    }

    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private void bitsandbalance$beginMoveCapture(Level level, BlockPos pos, Direction direction,
                                                  boolean extending,
                                                  CallbackInfoReturnable<Boolean> cir) {
        FabricPistonMoveCaptureContext.begin(level);
    }

    @WrapOperation(
            method = "moveBlocks",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;resolve()Z")
    )
    private boolean bitsandbalance$captureResolvedMoves(PistonStructureResolver resolver,
                                                         Operation<Boolean> original,
                                                         Level level,
                                                         BlockPos pistonPos,
                                                         Direction pistonDirection,
                                                         boolean extending) {
        boolean resolved = original.call(resolver);
        if (!resolved) {
            return false;
        }

        Direction moveDirection = extending ? pistonDirection : pistonDirection.getOpposite();
        for (BlockPos sourcePos : resolver.getToPush()) {
            BlockState movedState = level.getBlockState(sourcePos);
            FabricPistonMoveCaptureContext.stage(
                    level,
                    sourcePos,
                    sourcePos.relative(moveDirection),
                    movedState
            );
        }
        return true;
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private void bitsandbalance$clearMoveCapture(Level level, BlockPos pos, Direction direction,
                                                  boolean extending,
                                                  CallbackInfoReturnable<Boolean> cir) {
        FabricPistonMoveCaptureContext.clear();
    }
}
