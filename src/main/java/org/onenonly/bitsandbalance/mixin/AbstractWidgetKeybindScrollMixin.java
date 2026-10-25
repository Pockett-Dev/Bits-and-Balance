package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.client.KeybindAutoScrollSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveTextCollector.class)
public interface AbstractWidgetKeybindScrollMixin {
    @Inject(method = "defaultScrollingHelper", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$scrollLongKeybindTextWithShorterEndHolds(Component message, int centerX, int left, int right, int top, int bottom, int lineWidth, int lineHeight, ActiveTextCollector.Parameters parameters, CallbackInfo ci) {
        if (KeybindAutoScrollSpeed.acceptScrollingString((ActiveTextCollector) (Object) this, message, centerX, left, right, top, bottom, lineWidth, lineHeight, parameters)) {
            ci.cancel();
        }
    }
}