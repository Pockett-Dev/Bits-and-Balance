package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.ClientConfig;
import org.onenonly.bitsandbalance.client.KeybindModifierHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class ItemShareKeyMappingPresentationMixin {
    @Unique
    private static final String BITSANDBALANCE$ITEM_SHARE_MAPPING = "key.bitsandbalance.item_share";

    @Shadow
    public abstract InputConstants.Key getKey();

    @Inject(method = "getTranslatedKeyMessage", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$showEffectiveItemShareModifiers(CallbackInfoReturnable<Component> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!bitsandbalance$isItemShare(self)) {
            return;
        }

        int mask = bitsandbalance$effectiveMask(self);
        if (mask != 0) {
            cir.setReturnValue(KeybindModifierHelper.formatWithModifiers(this.getKey().getDisplayName(), mask));
        }
    }

    @Inject(method = "same", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$compareEffectiveItemShareModifiers(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping self = (KeyMapping) (Object) this;
        if (!bitsandbalance$isItemShare(self) && !bitsandbalance$isItemShare(other)) {
            return;
        }
        if (!this.getKey().equals(other.getKey())) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(bitsandbalance$effectiveMask(self) == bitsandbalance$effectiveMask(other));
    }

    @Unique
    private static boolean bitsandbalance$isItemShare(KeyMapping mapping) {
        return mapping != null && BITSANDBALANCE$ITEM_SHARE_MAPPING.equals(mapping.getName());
    }

    @Unique
    private static int bitsandbalance$effectiveMask(KeyMapping mapping) {
        boolean itemShare = bitsandbalance$isItemShare(mapping);
        return KeybindModifierHelper.effectiveModifierMask(
                mapping,
                0,
                itemShare && ClientConfig.itemShareRequireShift,
                itemShare && ClientConfig.itemShareRequireAlt,
                itemShare && ClientConfig.itemShareRequireCtrl
        );
    }
}
