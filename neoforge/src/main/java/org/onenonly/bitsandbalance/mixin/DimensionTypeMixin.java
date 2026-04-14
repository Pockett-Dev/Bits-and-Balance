package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.level.dimension.DimensionType;
import org.onenonly.bitsandbalance.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Respawn Anchor Anywhere
 *
 * Makes DimensionType#respawnAnchorWorks return true when enabled,
 * so vanilla respawn anchors behave like they do in the Nether.
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
    @Inject(method = "respawnAnchorWorks", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$respawnAnchorWorks(CallbackInfoReturnable<Boolean> cir) {
        if (Config.enableRespawnAnchorAnywhere) {
            cir.setReturnValue(true);
        }
    }
}
