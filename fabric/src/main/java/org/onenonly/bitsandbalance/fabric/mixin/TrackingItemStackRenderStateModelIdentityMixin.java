package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TrackingItemStackRenderState.class)
public abstract class TrackingItemStackRenderStateModelIdentityMixin {

    @Shadow
    private List<Object> modelIdentityElements;

    @Inject(method = "clear", at = @At("HEAD"), require = 0)
    private void bitsandbalance$clearModelIdentityElements(CallbackInfo ci) {
        if (this.modelIdentityElements != null) {
            this.modelIdentityElements.clear();
        }
    }

    @Inject(method = "getModelIdentity", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$returnImmutableModelIdentity(CallbackInfoReturnable<Object> cir) {
        if (this.modelIdentityElements == null || this.modelIdentityElements.isEmpty()) {
            cir.setReturnValue(List.of());
            return;
        }
        cir.setReturnValue(List.copyOf(this.modelIdentityElements));
    }
}
