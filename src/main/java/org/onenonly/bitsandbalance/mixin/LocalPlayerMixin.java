package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.onenonly.bitsandbalance.client.ClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Client mixins for LocalPlayer.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "isHandsBusy", at = @At("HEAD"), cancellable = true)
    private void rebalance$showHeldItemWhenRiding(CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableShowHeldItemWhenRiding) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.options.getCameraType().isFirstPerson()) return;
        LocalPlayer self = (LocalPlayer)(Object)this;
        if (self.isSpectator()) return;
        if (self.isPassenger()) {
            // Returning false means hands are not busy; this allows item-in-hand to render
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    private void rebalance$suppressCrouchWhenSitting(CallbackInfoReturnable<Boolean> cir) {
        try {
            LocalPlayer self = (LocalPlayer)(Object)this;
            if (ClientState.sitting || ClientState.SYNCED_SITTING_PLAYERS.contains(self.getUUID())) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {}
    }
}
