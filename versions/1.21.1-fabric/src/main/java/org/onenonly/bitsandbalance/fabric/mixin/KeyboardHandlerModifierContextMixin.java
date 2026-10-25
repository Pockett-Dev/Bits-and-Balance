package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyboardHandler;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerModifierContextMixin {
    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$setModifierContext(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        try {
            KeybindModifierContext.set(modifiers);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "keyPress(JIIII)V", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearModifierContext(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        KeybindModifierContext.clear();
    }
}