package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.onenonly.bitsandbalance.fabric.client.ModifierRebindHandler;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compatibility hook for the Controlling mod.
 *
 * Controlling replaces the Controls screen with its own {@code NewKeyBindsScreen} implementation,
 * and performs rebinding in its override of {@code keyPressed}. Vanilla (and Controlling's default)
 * will bind on the first key press, which makes it impossible to set Shift+Key combos on Fabric.
 *
 * This mixin mirrors our {@link KeyBindsScreenMixin} logic but targets Controlling's screen.
 */
@Pseudo
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsScreen")
public abstract class ControllingNewKeyBindsScreenMixin extends Screen implements ModifierRebindHandler {
    protected ControllingNewKeyBindsScreenMixin() {
        super(null);
    }

    @Unique
    private static final Logger BITSANDBALANCE_LOGGER = LoggerFactory.getLogger("bitsandbalance-keybinds");

    @Unique
    private int bitsandbalance$pendingModifierMask = 0;

    @Unique
    private InputConstants.Key bitsandbalance$pendingModifierBaseKey = InputConstants.UNKNOWN;

    @Unique
    private int bitsandbalance$pendingModifierBaseBit = 0;

    @Unique
    private boolean bitsandbalance$applyModifiersAfterBind = false;

    @Unique
    private int bitsandbalance$applyModifierMask = 0;

    @Unique
    private KeyMapping bitsandbalance$applyTargetKey = null;

    @Unique
    private KeyMapping bitsandbalance$lastSelectedKey = null;

    @Unique
    private int bitsandbalance$debugRebindEventsRemaining = 0;

    @Unique
    private KeyMapping bitsandbalance$getSelectedKey() {
        return ((KeyBindsScreenAccessor) (Object) this).bitsandbalance$getSelectedKey();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$captureModifierCombos_Controlling(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping selected = bitsandbalance$getSelectedKey();
        if (selected == null) {
            bitsandbalance$clearPendingRebindState();
            bitsandbalance$lastSelectedKey = null;
            bitsandbalance$debugRebindEventsRemaining = 0;
            return;
        }

        if (keyEvent.isEscape()) {
            bitsandbalance$clearPendingRebindState();
            return;
        }

        if (bitsandbalance$lastSelectedKey != selected) {
            bitsandbalance$lastSelectedKey = selected;
            bitsandbalance$debugRebindEventsRemaining = FabricClientConfig.debugKeybindRebindLogging ? 20 : 0;
        }

        if (bitsandbalance$debugRebindEventsRemaining > 0) {
            bitsandbalance$debugRebindEventsRemaining--;
            InputConstants.Key resolvedKey = InputConstants.getKey(keyEvent);
            BITSANDBALANCE_LOGGER.info(
                    "[B&B] Controlling rebind key event for '{}' -> key={}, keycode={}, mods={}, resolvedType={}, resolvedValue={}, resolvedName={}",
                    selected.getName(),
                    keyEvent.key(),
                    keyEvent.keycode(),
                    keyEvent.modifiers(),
                    resolvedKey.getType(),
                    resolvedKey.getValue(),
                    resolvedKey.getName()
            );
        }

        // If the user presses a modifier while rebinding, don't let Controlling bind to the modifier key.
        int modBit = bitsandbalance$modifierBitForKey(keyEvent);
        if (modBit != 0) {
            bitsandbalance$pendingModifierMask |= modBit;
            bitsandbalance$pendingModifierBaseKey = InputConstants.getKey(keyEvent);
            bitsandbalance$pendingModifierBaseBit = modBit;
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Non-modifier key: let Controlling bind the actual key, but remember modifiers.
        bitsandbalance$applyModifiersAfterBind = true;
        bitsandbalance$applyTargetKey = selected;
        bitsandbalance$applyModifierMask = bitsandbalance$pendingModifierMask | bitsandbalance$effectiveModifierMask(keyEvent);
        bitsandbalance$pendingModifierMask = 0;
    }

    @Unique
    private static int bitsandbalance$effectiveModifierMask(KeyEvent keyEvent) {
        try {
            if (keyEvent.modifiers() != 0) {
                return KeybindModifierStore.canonicalMask(keyEvent);
            }
            return bitsandbalance$currentModifierMask();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Inject(method = "keyPressed", at = @At("RETURN"), require = 0)
    private void bitsandbalance$applyModifierStoreAfterControllingBind(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!bitsandbalance$applyModifiersAfterBind) {
            return;
        }

        // Some Controlling versions refresh the UI list during keyPressed; in those cases we also
        // inject *before* the refresh call (see below). This RETURN injection is a fallback.
        bitsandbalance$applyModifiersAfterBind = false;

        int mask = bitsandbalance$applyModifierMask;
        bitsandbalance$applyModifierMask = 0;

        KeyMapping target = bitsandbalance$applyTargetKey;
        bitsandbalance$applyTargetKey = null;
        if (target == null) {
            return;
        }

        KeybindModifierStore.setRequiredMask(target, mask);

        // Force the controls list to re-render its button labels immediately.
        bitsandbalance$forceKeybindListRefresh();
    }

    @Inject(
            method = "keyPressed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;resetMappingAndUpdateButtons()V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void bitsandbalance$applyModifierStoreBeforeUiRefresh_Controlling(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!bitsandbalance$applyModifiersAfterBind) {
            return;
        }
        bitsandbalance$applyModifiersAfterBind = false;

        int mask = bitsandbalance$applyModifierMask;
        bitsandbalance$applyModifierMask = 0;

        KeyMapping target = bitsandbalance$applyTargetKey;
        bitsandbalance$applyTargetKey = null;
        if (target == null) {
            return;
        }

        KeybindModifierStore.setRequiredMask(target, mask);
    }

    @Override
    public boolean bitsandbalance$finishPendingModifierRebind(KeyEvent keyEvent) {
        KeyMapping selected = bitsandbalance$getSelectedKey();
        if (selected == null) {
            return false;
        }

        int modBit = bitsandbalance$modifierBitForKey(keyEvent);
        if (modBit == 0) {
            return false;
        }

        int pendingMask = bitsandbalance$pendingModifierMask & KeybindModifierStore.MOD_MASK;
        if (pendingMask == 0) {
            return false;
        }

        InputConstants.Key baseKey = bitsandbalance$pendingModifierBaseKey;
        int baseBit = bitsandbalance$pendingModifierBaseBit;
        if (InputConstants.UNKNOWN.equals(baseKey) || baseBit == 0 || (pendingMask & baseBit) == 0) {
            baseKey = InputConstants.getKey(keyEvent);
            baseBit = modBit;
        }

        bitsandbalance$completeBinding(selected, baseKey, pendingMask & ~baseBit);
        return true;
    }

    @Unique
    private void bitsandbalance$completeBinding(KeyMapping target, InputConstants.Key baseKey, int requiredMask) {
        bitsandbalance$clearPendingRebindState();
        target.setKey(baseKey);
        KeybindModifierStore.setRequiredMask(target, requiredMask);
        KeyBindsScreen keyBindsScreen = (KeyBindsScreen) (Object) this;
        keyBindsScreen.selectedKey = null;
        keyBindsScreen.lastKeySelection = Util.getMillis();
        bitsandbalance$forceKeybindListRefresh();
    }

    @Unique
    private void bitsandbalance$clearPendingRebindState() {
        bitsandbalance$pendingModifierMask = 0;
        bitsandbalance$pendingModifierBaseKey = InputConstants.UNKNOWN;
        bitsandbalance$pendingModifierBaseBit = 0;
        bitsandbalance$applyModifiersAfterBind = false;
        bitsandbalance$applyModifierMask = 0;
        bitsandbalance$applyTargetKey = null;
    }

    @Unique
    private static int bitsandbalance$currentModifierMask() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return 0;
        }

        boolean shiftDown = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
        boolean altDown = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(InputConstants.KEY_RALT);
        boolean ctrlDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);

        int mask = 0;
        if (shiftDown) mask |= KeybindModifierStore.MOD_SHIFT;
        if (altDown) mask |= KeybindModifierStore.MOD_ALT;
        if (ctrlDown) mask |= KeybindModifierStore.MOD_CONTROL;
        return mask;
    }

    @Unique
    private void bitsandbalance$forceKeybindListRefresh() {
        try {
            var list = ((KeyBindsScreenAccessor) (Object) this).bitsandbalance$getKeyBindsList();
            if (list != null) {
                list.resetMappingAndUpdateButtons();
            }
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static int bitsandbalance$modifierBitForKey(KeyEvent keyEvent) {
        return switch (keyEvent.key()) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> KeybindModifierStore.MOD_SHIFT;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> KeybindModifierStore.MOD_ALT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> KeybindModifierStore.MOD_CONTROL;
            default -> 0;
        };
    }
}
