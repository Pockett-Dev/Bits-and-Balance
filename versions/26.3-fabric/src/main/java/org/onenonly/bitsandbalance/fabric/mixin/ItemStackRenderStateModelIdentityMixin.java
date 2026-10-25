package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.onenonly.bitsandbalance.fabric.access.TrackingItemStackRenderStateModelIdentityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateModelIdentityMixin {
    @Inject(method = "clear", at = @At("TAIL"), require = 1)
    private void bitsandbalance$clearTrackingModelIdentity(CallbackInfo ci) {
        if ((Object) this instanceof TrackingItemStackRenderStateModelIdentityAccess tracking) {
            tracking.bitsandbalance$clearModelIdentityElements();
        }
    }
}
