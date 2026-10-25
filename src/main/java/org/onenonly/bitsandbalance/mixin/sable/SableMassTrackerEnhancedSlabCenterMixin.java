package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3dc;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.physics.mass.MassTracker$1", remap = false)
public abstract class SableMassTrackerEnhancedSlabCenterMixin {

    @Inject(
            method = "lambda$apply$0(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;I)Lorg/joml/Vector3dc;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void bitsandbalance$useEnhancedSlabCenter(BlockState state,
                                                             BlockGetter level,
                                                             int cacheKey,
                                                             CallbackInfoReturnable<Vector3dc> cir) {
        Vector3dc center = EnhancedSlabCreateCompat.getSableCenterOfMass(level, state);
        if (center != null) {
            cir.setReturnValue(center);
        }
    }
}
