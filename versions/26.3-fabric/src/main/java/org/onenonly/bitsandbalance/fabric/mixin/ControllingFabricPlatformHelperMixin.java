package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Compatibility hook that lets Controlling recognize SDL3 modifier keys. */
@Pseudo
@Mixin(targets = "com.blamejared.controlling.platform.FabricPlatformHelper", remap = false)
public class ControllingFabricPlatformHelperMixin {

    @Inject(method = "isKeyCodeModifier", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$isKeyCodeModifier(InputConstants.Key key, CallbackInfoReturnable<Boolean> cir) {
        if (key == null || key.getType() != InputConstants.Type.KEYBOARD) {
            return;
        }

        int value = key.getValue();
        if (value == InputConstants.KEY_LSHIFT || value == InputConstants.KEY_RSHIFT
                || value == InputConstants.KEY_LALT || value == InputConstants.KEY_RALT
                || value == InputConstants.KEY_LCONTROL || value == InputConstants.KEY_RCONTROL) {
            cir.setReturnValue(true);
        }
    }
}
