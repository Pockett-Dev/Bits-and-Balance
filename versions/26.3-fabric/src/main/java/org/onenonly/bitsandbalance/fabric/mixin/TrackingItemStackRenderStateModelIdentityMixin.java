package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.onenonly.bitsandbalance.fabric.access.TrackingItemStackRenderStateModelIdentityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(TrackingItemStackRenderState.class)
public abstract class TrackingItemStackRenderStateModelIdentityMixin
        implements TrackingItemStackRenderStateModelIdentityAccess {

    @Shadow
    @Final
    private List<Object> modelIdentityElements;

    @Override
    public void bitsandbalance$clearModelIdentityElements() {
        modelIdentityElements.clear();
    }

    @Inject(method = "getModelIdentity", at = @At("HEAD"), cancellable = true, require = 1)
    private void bitsandbalance$returnImmutableModelIdentity(CallbackInfoReturnable<Object> cir) {
        if (modelIdentityElements.isEmpty()) {
            cir.setReturnValue(List.of());
            return;
        }

        List<Object> snapshot = new ArrayList<>(modelIdentityElements.size());
        for (Object element : modelIdentityElements) {
            if (element != null) {
                snapshot.add(element);
            }
        }
        cir.setReturnValue(snapshot.isEmpty() ? List.of() : List.copyOf(snapshot));
    }
}
