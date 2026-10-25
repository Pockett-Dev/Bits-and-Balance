package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDreamGlideMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$prepareDreamGhost(CallbackInfo ci) {
        DreamProjectionClient.prepareGlide((LocalPlayer) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$glideDreamGhost(CallbackInfo ci) {
        DreamProjectionClient.tickGlide((LocalPlayer) (Object) this);
    }
}
