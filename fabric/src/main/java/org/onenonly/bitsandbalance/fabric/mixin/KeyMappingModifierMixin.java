package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds Amecs-like modifier support to all keybindings.
 *
 * We persist required modifier masks separately and enforce them at runtime by
 * filtering {@link KeyMapping#matches(KeyEvent)}.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingModifierMixin {
    @Shadow
    protected InputConstants.Key key;

    @Shadow
    public abstract String getName();

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$enforceModifierMasks(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        int eventMods = keyEvent.modifiers() & KeybindModifierStore.MOD_MASK;

        if (required == 0) {
            // If a more specific binding exists for this same base key, let it win.
            if (eventMods != 0 && bitsandbalance$existsMoreSpecificMatch(eventMods, 0, self)) {
                cir.setReturnValue(false);
            }
            return;
        }

        // Must have at least the required modifiers.
        if ((eventMods & required) != required) {
            cir.setReturnValue(false);
            return;
        }

        // If a stricter (superset) modifier binding also matches, suppress this one.
        if (bitsandbalance$existsMoreSpecificMatch(eventMods, required, self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isDown", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$requireModifiersForIsDown(CallbackInfoReturnable<Boolean> cir) {
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

    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$decorateKeyNameWithModifiers(CallbackInfoReturnable<Component> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required == 0) {
            return;
        }
        cir.setReturnValue(KeybindModifierStore.formatWithModifiers(cir.getReturnValue(), required));
    }

    private static boolean bitsandbalance$existsMoreSpecificMatch(int eventMods, int currentRequired, KeyMapping self) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return false;
        }

        InputConstants.Key baseKey = ((KeyMappingAccessor) (Object) self).bitsandbalance$getKey();
        if (baseKey == null) {
            return false;
        }

        for (KeyMapping other : mc.options.keyMappings) {
            if (other == self) {
                continue;
            }
            InputConstants.Key otherKey = ((KeyMappingAccessor) (Object) other).bitsandbalance$getKey();
            if (otherKey == null || !otherKey.equals(baseKey)) {
                continue;
            }

            int otherRequired = KeybindModifierStore.getRequiredMask(other);
            if (otherRequired == 0) {
                continue;
            }

            // otherRequired must be a strict superset of currentRequired (or just non-zero when currentRequired is 0)
            if ((otherRequired & currentRequired) != currentRequired) {
                continue;
            }
            if (otherRequired == currentRequired) {
                continue;
            }

            if ((eventMods & otherRequired) == otherRequired) {
                return true;
            }
        }
        return false;
    }
}
