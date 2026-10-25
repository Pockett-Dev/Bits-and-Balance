package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.client.ModifierRebindHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerModifierContextMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), require = 0)
    private void bitsandbalance$setModifierContext(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        try {
            KeybindModifierContext.set(KeybindModifierStore.canonicalMask(keyEvent));
        } catch (Throwable ignored) {
        }

        if (action != InputConstants.RELEASE) {
            return;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || !(mc.gui.screen() instanceof ModifierRebindHandler handler)) {
                return;
            }

            handler.bitsandbalance$finishPendingModifierRebind(keyEvent);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "keyPress", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearModifierContext(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        KeybindModifierContext.clear();
    }
}
