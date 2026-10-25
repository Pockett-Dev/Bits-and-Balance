package org.onenonly.bitsandbalance.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.impl.contraption.BlockMovementChecksImpl", remap = false)
public abstract class CreateBlockMovementChecksEnhancedSlabMixin {
    @Inject(method = "isMovementNecessary", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bitsandbalance$forceEnhancedSlabMovement(BlockState state,
                                                                 Level level,
                                                                 BlockPos pos,
                                                                 CallbackInfoReturnable<Boolean> cir) {
        if (EnhancedSlabCreateCompat.supports(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isNotSupportive", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bitsandbalance$keepEnhancedSlabFacesSupportive(BlockState state,
                                                                       Direction direction,
                                                                       CallbackInfoReturnable<Boolean> cir) {
        if (EnhancedSlabCreateCompat.supports(state)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isBlockAttachedTowards", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void bitsandbalance$treatOccupiedEnhancedSlabFacesAsAttached(BlockState state,
                                                                                Level level,
                                                                                BlockPos pos,
                                                                                Direction direction,
                                                                                CallbackInfoReturnable<Boolean> cir) {
        if (EnhancedSlabCreateCompat.supports(state)) {
            cir.setReturnValue(EnhancedSlabCreateCompat.isAttachedTowards(state, level, pos, direction));
        }
    }
}
