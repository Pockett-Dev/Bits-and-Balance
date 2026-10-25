package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.MouseHandler;
import org.onenonly.bitsandbalance.common.client.ArrowPriorityScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Redirect the wheel from hotbar cycling to arrow selection while the HUD selector is held. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerArrowPriorityMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$selectArrow(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ArrowPriorityScreen.onScroll(vertical)) {
            ci.cancel();
        }
    }
}
