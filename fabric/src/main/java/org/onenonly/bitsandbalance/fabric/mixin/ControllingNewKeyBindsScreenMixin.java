package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings;
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
public abstract class ControllingNewKeyBindsScreenMixin extends Screen {
    protected ControllingNewKeyBindsScreenMixin() {
        super(null);
    }

    @Unique
    private static final Logger BITSANDBALANCE_LOGGER = LoggerFactory.getLogger("bitsandbalance-keybinds");

    @Unique
    private int bitsandbalance$pendingModifierMask = 0;

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
            bitsandbalance$pendingModifierMask = 0;
            bitsandbalance$lastSelectedKey = null;
            bitsandbalance$debugRebindEventsRemaining = 0;
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
                    "[B&B] Controlling rebind key event for '{}' -> key={}, scancode={}, mods={}, resolvedType={}, resolvedValue={}, resolvedName={}",
                    selected.getName(),
                    keyEvent.key(),
                    keyEvent.scancode(),
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
            int mods = keyEvent.modifiers();
            if (mods != 0) {
                return mods;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                return 0;
            }

            var window = mc.getWindow();
            boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
            boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

            int mask = 0;
            if (shiftDown) mask |= GLFW.GLFW_MOD_SHIFT;
            if (altDown) mask |= GLFW.GLFW_MOD_ALT;
            if (ctrlDown) mask |= GLFW.GLFW_MOD_CONTROL;
            return mask;
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
        // Prefer the raw keycode if present.
        int rawKey = keyEvent.key();
        if (rawKey != -1) {
            return switch (rawKey) {
                case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
                case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
                case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        // KeyEvent.key() can be -1 for some keys (scancode-only).
        InputConstants.Key key = InputConstants.getKey(keyEvent);

        if (key.getType() == InputConstants.Type.KEYSYM) {
            return switch (key.getValue()) {
                case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
                case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
                case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        if (key.getType() == InputConstants.Type.SCANCODE) {
            return switch (key.getValue()) {
                // Shift
                case 42, 54 -> GLFW.GLFW_MOD_SHIFT;
                // Alt
                case 56, 312 -> GLFW.GLFW_MOD_ALT;
                // Control
                case 29, 285 -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        int rawScancode = keyEvent.scancode();
        if (rawScancode != -1) {
            return switch (rawScancode) {
                // Shift
                case 42, 54 -> GLFW.GLFW_MOD_SHIFT;
                // Alt
                case 56, 312 -> GLFW.GLFW_MOD_ALT;
                // Control
                case 29, 285 -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        return 0;
    }
}
