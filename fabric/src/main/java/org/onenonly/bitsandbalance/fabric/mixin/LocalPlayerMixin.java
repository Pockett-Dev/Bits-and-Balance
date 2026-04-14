package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client mixins for LocalPlayer.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "isHandsBusy", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$showHeldItemWhenRiding(CallbackInfoReturnable<Boolean> cir) {
        if (!FabricClientConfig.enableShowHeldItemWhenRiding) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.options.getCameraType().isFirstPerson()) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.isSpectator()) return;

        if (self.isPassenger()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$suppressCrouchWhenSitting(CallbackInfoReturnable<Boolean> cir) {
        try {
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (self.isPassenger()) return;
            if (FabricClientState.sitting || FabricClientState.SYNCED_SITTING_PLAYERS.contains(self.getUUID())) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }
}
