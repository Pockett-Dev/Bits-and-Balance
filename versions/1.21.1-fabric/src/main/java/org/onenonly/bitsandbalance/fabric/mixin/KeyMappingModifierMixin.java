package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingModifierMixin {
    @Shadow
    protected InputConstants.Key key;

    @Inject(method = "matches(II)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$enforceModifierMasks(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        int eventMods = KeybindModifierContext.get() & KeybindModifierStore.MOD_MASK;

        if (required == 0) {
            if (eventMods != 0 && bitsandbalance$existsMoreSpecificMatch(eventMods, 0, self)) {
                cir.setReturnValue(false);
            }
            return;
        }

        if ((eventMods & required) != required) {
            cir.setReturnValue(false);
            return;
        }

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
        if (required != 0 && !KeybindModifierStore.areRequiredModifiersDown(required)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$decorateKeyNameWithModifiers(CallbackInfoReturnable<Component> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        int required = KeybindModifierStore.getRequiredMask(self);
        if (required != 0) {
            cir.setReturnValue(KeybindModifierStore.formatWithModifiers(cir.getReturnValue(), required));
        }
    }

    @Inject(method = "isDefault", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$includeModifierMaskInDefaultState(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        if (KeybindModifierStore.getRequiredMask(self) != 0) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "same", at = @At("RETURN"), cancellable = true, require = 0)
    private void bitsandbalance$includeModifierMaskInConflictChecks(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        KeyMapping self = (KeyMapping) (Object) this;
        if (KeybindModifierStore.getRequiredMask(self) != KeybindModifierStore.getRequiredMask(other)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setKey", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearModifiersWhenKeyResets(InputConstants.Key key, CallbackInfo ci) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (InputConstants.UNKNOWN.equals(key) || key.equals(self.getDefaultKey())) {
            KeybindModifierStore.setRequiredMask(self, 0);
        }
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
            if ((otherRequired & currentRequired) != currentRequired || otherRequired == currentRequired) {
                continue;
            }
            if ((eventMods & otherRequired) == otherRequired) {
                return true;
            }
        }
        return false;
    }
}