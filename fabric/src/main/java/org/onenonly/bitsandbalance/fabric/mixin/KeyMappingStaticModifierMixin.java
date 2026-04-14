package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierContext;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * Filters vanilla keybinding updates (click/down state) using our stored modifier masks.
 *
 * Without this, pressing "L" will increment clickCount for *all* keybinds bound to L,
 * regardless of whether they require Shift/Alt/Ctrl.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingStaticModifierMixin {
    @Shadow
    @Final
    private static Map<InputConstants.Key, List<KeyMapping>> MAP;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$filterSet(InputConstants.Key key, boolean down, CallbackInfo ci) {
        // Let vanilla handle releases to avoid stuck keys.
        if (!down) {
            return;
        }

        List<KeyMapping> list = MAP.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }

        int eventMods = KeybindModifierContext.get() & KeybindModifierStore.MOD_MASK;

        int bestBits = -1;
        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            if ((eventMods & required) == required) {
                bestBits = Math.max(bestBits, Integer.bitCount(required));
            }
        }

        // If nothing matches, keep vanilla behavior.
        if (bestBits < 0) {
            return;
        }

        ci.cancel();

        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            boolean match = (eventMods & required) == required && Integer.bitCount(required) == bestBits;
            mapping.setDown(match);
        }
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$filterClick(InputConstants.Key key, CallbackInfo ci) {
        List<KeyMapping> list = MAP.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }

        int eventMods = KeybindModifierContext.get() & KeybindModifierStore.MOD_MASK;

        int bestBits = -1;
        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            if ((eventMods & required) == required) {
                bestBits = Math.max(bestBits, Integer.bitCount(required));
            }
        }

        if (bestBits < 0) {
            return;
        }

        ci.cancel();

        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            boolean match = (eventMods & required) == required && Integer.bitCount(required) == bestBits;
            if (!match) {
                continue;
            }
            KeyMappingClickCountAccessor acc = (KeyMappingClickCountAccessor) (Object) mapping;
            acc.bitsandbalance$setClickCount(acc.bitsandbalance$getClickCount() + 1);
        }
    }
}
