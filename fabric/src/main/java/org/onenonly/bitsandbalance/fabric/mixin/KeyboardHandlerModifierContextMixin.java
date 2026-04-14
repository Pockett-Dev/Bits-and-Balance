package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerModifierContextMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), require = 0)
    private void bitsandbalance$setModifierContext(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        try {
            KeybindModifierContext.set(keyEvent.modifiers());
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "keyPress", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearModifierContext(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        KeybindModifierContext.clear();
    }
}
