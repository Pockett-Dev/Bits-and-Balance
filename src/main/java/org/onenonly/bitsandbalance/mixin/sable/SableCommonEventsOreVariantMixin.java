package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.SableCommonEvents", remap = false)
public abstract class SableCommonEventsOreVariantMixin {
    @Inject(method = "handleBlockChange", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void bitsandbalance$skipGeneratedOreVariantPhysics(ServerLevel level,
                                                                      LevelChunk chunk,
                                                                      int x,
                                                                      int y,
                                                                      int z,
                                                                      BlockState oldState,
                                                                      BlockState newState,
                                                                      CallbackInfo ci) {
        if (SableCompat.isOreVariantReplacementActive()
                || (SableCompat.isPlotBlockWriteActive() && oldState.isAir() == newState.isAir())) {
            ci.cancel();
        }
    }
}
