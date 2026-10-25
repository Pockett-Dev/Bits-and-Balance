package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.onenonly.bitsandbalance.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class KeyMappingMultiModifierMixin {
    @Shadow
    private int clickCount;

    @Shadow
    public InputConstants.Key getKey() {
        throw new AssertionError();
    }

    @Inject(method = "setKey", at = @At("RETURN"))
    private void bitsandbalance$clearModifiersOnSetKey(InputConstants.Key key, CallbackInfo ci) {
        KeybindModifierStore.clearStoredMask((KeyMapping) (Object) this);
    }

    @Inject(method = "setKeyModifierAndCode", at = @At("RETURN"))
    private void bitsandbalance$clearModifiersOnSetKeyModifierAndCode(KeyModifier modifier, InputConstants.Key key, CallbackInfo ci) {
        KeybindModifierStore.clearStoredMask((KeyMapping) (Object) this);
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$requireStoredModifiersForDown(CallbackInfoReturnable<Boolean> cir) {
        if (!KeybindModifierStore.areStoredModifiersDown((KeyMapping) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$requireStoredModifiersForClick(CallbackInfoReturnable<Boolean> cir) {
        if (!KeybindModifierStore.areStoredModifiersDown((KeyMapping) (Object) this)) {
            this.clickCount = 0;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTranslatedKeyMessage", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$showStoredModifiers(CallbackInfoReturnable<Component> cir) {
        if (KeybindModifierStore.getEffectiveModifierMask((KeyMapping) (Object) this) != 0) {
            cir.setReturnValue(KeybindModifierStore.formatStoredModifiers((KeyMapping) (Object) this, this.getKey().getDisplayName()));
        }
    }

    @Inject(method = "same", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$compareEffectiveItemShareModifiers(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!KeybindModifierStore.isItemShareMapping(self) && !KeybindModifierStore.isItemShareMapping(other)) {
            return;
        }
        if (!this.getKey().equals(other.getKey())) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(KeybindModifierStore.getEffectiveModifierMask(self)
                == KeybindModifierStore.getEffectiveModifierMask(other));
    }

    @Inject(method = "isDefault", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$extraModifiersAreNotDefault(CallbackInfoReturnable<Boolean> cir) {
        if (KeybindModifierStore.getStoredMask((KeyMapping) (Object) this) != 0) {
            cir.setReturnValue(false);
        }
    }
}
