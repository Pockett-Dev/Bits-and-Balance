package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.player.RemotePlayer;
import org.onenonly.bitsandbalance.client.ClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Client: suppress crouch animation for remote players marked as sitting.
 */
@Mixin(RemotePlayer.class)
public abstract class RemotePlayerMixin {

    @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    private void rebalance$suppressCrouchWhenSitting(CallbackInfoReturnable<Boolean> cir) {
        try {
            UUID id = ((RemotePlayer)(Object)this).getUUID();
            if (ClientState.SYNCED_SITTING_PLAYERS.contains(id)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {}
    }
}
