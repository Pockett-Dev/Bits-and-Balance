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

import java.util.ArrayList;
import java.util.Collections;
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
    private static Map<InputConstants.Key, Object> MAP;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$filterSet(InputConstants.Key key, boolean down, CallbackInfo ci) {
        // Let vanilla handle releases to avoid stuck keys.
        if (!down) {
            return;
        }

        List<KeyMapping> list = bitsandbalance$getMappings(key);
        if (list == null || list.isEmpty()) {
            return;
        }

        int eventMods = KeybindModifierContext.get() & KeybindModifierStore.MOD_MASK;

        int bestBits = -1;
        boolean hasModifierRestrictedBinding = false;
        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            if (required != 0) {
                hasModifierRestrictedBinding = true;
            }
            if ((eventMods & required) == required) {
                bestBits = Math.max(bestBits, Integer.bitCount(required));
            }
        }

        // Preserve vanilla and other mods' key-resolution hooks unless this key
        // actually has a B&B modifier-restricted binding to disambiguate.
        if (!hasModifierRestrictedBinding) {
            return;
        }

        if (bestBits < 0) {
            ci.cancel();
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
        List<KeyMapping> list = bitsandbalance$getMappings(key);
        if (list == null || list.isEmpty()) {
            return;
        }

        int eventMods = KeybindModifierContext.get() & KeybindModifierStore.MOD_MASK;

        int bestBits = -1;
        boolean hasModifierRestrictedBinding = false;
        for (KeyMapping mapping : list) {
            int required = KeybindModifierStore.getRequiredMask(mapping);
            if (required != 0) {
                hasModifierRestrictedBinding = true;
            }
            if ((eventMods & required) == required) {
                bestBits = Math.max(bestBits, Integer.bitCount(required));
            }
        }

        if (!hasModifierRestrictedBinding) {
            return;
        }

        if (bestBits < 0) {
            ci.cancel();
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

    private static List<KeyMapping> bitsandbalance$getMappings(InputConstants.Key key) {
        Object value = MAP.get(key);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof KeyMapping mapping) {
            return Collections.singletonList(mapping);
        }
        if (!(value instanceof List<?> rawList) || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<KeyMapping> mappings = new ArrayList<>(rawList.size());
        for (Object entry : rawList) {
            if (entry instanceof KeyMapping mapping) {
                mappings.add(mapping);
            }
        }
        return mappings;
    }
}
