package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ToggleKeyMapping overrides {@link KeyMapping#isDown()} and can bypass our modifier enforcement.
 *
 * This keeps modifier-required toggle keybinds (like Sit) consistent with normal keybinds.
 */
@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMappingModifierMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$preventToggleWithoutModifiers(boolean down, CallbackInfo ci) {
        if (!down) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required == 0) {
            return;
        }

        if (!KeybindModifierStore.areRequiredModifiersDown(required)) {
            ci.cancel();
        }
    }

    // ToggleKeyMapping doesn't necessarily declare isDown()/getTranslatedKeyMessage() itself in bytecode,
    // so these injections must be optional to avoid startup crashes.
    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$requireModifiersForToggleIsDown(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required == 0) {
            return;
        }
        if (!KeybindModifierStore.areRequiredModifiersDown(required)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$decorateToggleKeyNameWithModifiers(CallbackInfoReturnable<Component> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required == 0) {
            return;
        }
        cir.setReturnValue(KeybindModifierStore.formatWithModifiers(cir.getReturnValue(), required));
    }
}
