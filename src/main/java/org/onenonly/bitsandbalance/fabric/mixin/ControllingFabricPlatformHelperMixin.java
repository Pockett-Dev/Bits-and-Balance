package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compatibility hook for the Controlling mod on Fabric.
 *
 * Controlling delegates "is this key a modifier" to its platform helper.
 * On Fabric, their helper can return false for modifier keys (depending on event availability),
 * which causes the rebind flow to immediately finalize on Shift/Ctrl/Alt.
 *
 * We patch the platform helper to treat standard modifier keys as modifiers.
 */
@Pseudo
@Mixin(targets = "com.blamejared.controlling.platform.FabricPlatformHelper", remap = false)
public class ControllingFabricPlatformHelperMixin {

    @Inject(method = "isKeyCodeModifier", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$isKeyCodeModifier(InputConstants.Key key, CallbackInfoReturnable<Boolean> cir) {
        if (key == null) {
            return;
        }

        // Prefer stable GLFW keysyms when available.
        if (key.getType() == InputConstants.Type.KEYSYM) {
            int value = key.getValue();
            if (value == GLFW.GLFW_KEY_LEFT_SHIFT || value == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || value == GLFW.GLFW_KEY_LEFT_ALT || value == GLFW.GLFW_KEY_RIGHT_ALT
                    || value == GLFW.GLFW_KEY_LEFT_CONTROL || value == GLFW.GLFW_KEY_RIGHT_CONTROL) {
                cir.setReturnValue(true);
            }
            return;
        }

        // Some layouts/devices report modifiers as scancodes.
        if (key.getType() == InputConstants.Type.SCANCODE) {
            int scancode = key.getValue();
            // Shift: 42/54, Alt: 56/312, Ctrl: 29/285 (common on Windows/Linux)
            if (scancode == 42 || scancode == 54 || scancode == 56 || scancode == 312 || scancode == 29 || scancode == 285) {
                cir.setReturnValue(true);
            }
        }
    }
}
