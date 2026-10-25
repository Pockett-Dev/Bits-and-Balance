package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveHooks;
import org.onenonly.bitsandbalance.common.mixin.EnhancedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.MixedSlabMovingPistonAccess;
import org.onenonly.bitsandbalance.common.mixin.PistonBlockEntityMovingAccess;
import org.onenonly.bitsandbalance.fabric.compat.FabricCarpetPistonCompat;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricPistonMoveCaptureContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attaches captured data to whichever mod creates the real moving piston BE. */
@Mixin(MovingPistonBlock.class)
@SuppressWarnings({"null", "UnusedMethod"})
public abstract class MovingPistonBlockCaptureMixin {

    @Inject(method = "newMovingBlockEntity", at = @At("RETURN"))
    private static void bitsandbalance$attachCapturedMoveData(
            BlockPos destPos,
            BlockState movingPistonState,
            BlockState movedBlockState,
            Direction direction,
            boolean extending,
            boolean isHead,
            CallbackInfoReturnable<BlockEntity> cir) {
        BlockEntity result = cir.getReturnValue();
        if (result == null || movedBlockState == null || isHead) {
            return;
        }

        FabricPistonMoveCaptureContext.PendingMove pending =
                FabricPistonMoveCaptureContext.take(destPos, movedBlockState);
        if (pending == null) {
            return;
        }

        if (pending.bottomSlab() != null && pending.topSlab() != null
                && result instanceof MixedSlabMovingPistonAccess access) {
            access.bitsandbalance$setMixedSlabData(pending.bottomSlab(), pending.topSlab());
        }
        if (pending.enhancedSlabData() != null
                && result instanceof EnhancedSlabMovingPistonAccess access) {
            access.bitsandbalance$setEnhancedSlabPistonData(pending.enhancedSlabData());
        }

        CompoundTag tag = pending.blockEntityTag();
        Level level = FabricPistonMoveCaptureContext.level();
        if (tag != null && level != null && !FabricCarpetPistonCompat.ownsGenericBlockEntityMovement()) {
            Direction moveDirection = extending ? direction : direction.getOpposite();
            BlockPos sourcePos = destPos.relative(moveDirection.getOpposite());
            CompoundTag detached = PistonBlockEntityMoveHooks.captureAndDetach(level, sourcePos);
            if (detached != null && !detached.isEmpty()) {
                tag = detached;
                PistonBlockEntityMoveHooks.stage(level, sourcePos, destPos, tag);
            }
            if (result instanceof PistonBlockEntityMovingAccess access) {
                access.bitsandbalance$setMovedBlockEntityTag(tag);
            }
        }
    }
}
