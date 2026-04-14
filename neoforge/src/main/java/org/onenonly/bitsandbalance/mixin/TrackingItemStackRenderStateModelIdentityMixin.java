package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fixes GUI item-atlas caching for items that rely on special renderer arguments (e.g. decorated shields).
 *
 * Vanilla/loom TrackingItemStackRenderState exposes a mutable List as the model-identity key.
 * That list is then used as a HashMap key in GuiRenderer; mutating it between frames can cause
 * stale atlas entries to be reused (e.g. undecorated shield snapshot reused for a decorated shield).
 *
 * We make the identity immutable-by-value (copy) and ensure the underlying accumulator is cleared
 * whenever the render state is cleared.
 */
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
        // Snapshot so callers can safely use it as a cache key.
        cir.setReturnValue(List.copyOf(this.modelIdentityElements));
    }
}
