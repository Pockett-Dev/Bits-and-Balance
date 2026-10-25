package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps modifier-required toggle keybinds consistent with normal keybinds. */
@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMappingModifierMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true, require = 1)
    private void bitsandbalance$preventToggleWithoutModifiers(boolean down, CallbackInfo ci) {
        if (!down) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required != 0 && !KeybindModifierStore.areRequiredModifiersDown(required)) {
            ci.cancel();
        }
    }
}
