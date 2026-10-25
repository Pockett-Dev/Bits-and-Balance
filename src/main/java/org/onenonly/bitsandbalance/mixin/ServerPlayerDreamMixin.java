package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDreamMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$clearDreamBodyInput(CallbackInfo ci) {
        ServerPlayer body = (ServerPlayer) (Object) this;
        if (DreamProjection.isProjected(body)) {
            body.xxa = 0.0F;
            body.yya = 0.0F;
            body.zza = 0.0F;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$tickDream(CallbackInfo ci) {
        DreamProjection.tick((ServerPlayer) (Object) this);
    }
}
