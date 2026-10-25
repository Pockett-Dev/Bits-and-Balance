package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.fabric.client.KeybindModifierStore;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsScreen")
public abstract class ControllingNewKeyBindsScreenMixin extends Screen {
    protected ControllingNewKeyBindsScreenMixin() {
        super(Component.empty());
    }

    @Unique
    private static final Logger BITSANDBALANCE_LOGGER = LoggerFactory.getLogger("bitsandbalance-keybinds");

    @Unique
    private int bitsandbalance$pendingModifierMask = 0;

    @Unique
    private int bitsandbalance$heldModifierMask = 0;

    @Unique
    private int bitsandbalance$lastModifierBit = 0;

    @Unique
    private InputConstants.Key bitsandbalance$lastModifierKey = InputConstants.UNKNOWN;

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

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$captureModifierCombos_Controlling(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyMapping selected = bitsandbalance$getSelectedKey();
        if (selected == null) {
            bitsandbalance$clearPendingModifiers();
            bitsandbalance$lastSelectedKey = null;
            bitsandbalance$debugRebindEventsRemaining = 0;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            bitsandbalance$clearPendingModifiers();
            return;
        }

        if (bitsandbalance$lastSelectedKey != selected) {
            bitsandbalance$lastSelectedKey = selected;
            bitsandbalance$debugRebindEventsRemaining = FabricClientConfig.debugKeybindRebindLogging ? 20 : 0;
        }

        if (bitsandbalance$debugRebindEventsRemaining > 0) {
            bitsandbalance$debugRebindEventsRemaining--;
            InputConstants.Key resolvedKey = InputConstants.getKey(keyCode, scanCode);
            BITSANDBALANCE_LOGGER.info(
                "[B&B] Controlling rebind key event for '{}' -> key={}, scancode={}, mods={}, resolvedType={}, resolvedValue={}, resolvedName={}",
                selected.getName(),
                keyCode,
                scanCode,
                modifiers,
                resolvedKey.getType(),
                resolvedKey.getValue(),
                resolvedKey.getName()
            );
        }

        int modBit = bitsandbalance$modifierBitForKey(keyCode, scanCode);
        if (modBit != 0) {
            bitsandbalance$pendingModifierMask |= modBit;
            bitsandbalance$heldModifierMask |= modBit;
            bitsandbalance$lastModifierBit = modBit;
            bitsandbalance$lastModifierKey = InputConstants.getKey(keyCode, scanCode);
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        bitsandbalance$applyModifiersAfterBind = true;
        bitsandbalance$applyTargetKey = selected;
        bitsandbalance$applyModifierMask = bitsandbalance$pendingModifierMask | bitsandbalance$effectiveModifierMask(modifiers);
        bitsandbalance$clearPendingModifiers();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$completeModifierOnlyBindAfterRelease_Controlling(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        KeyMapping selected = bitsandbalance$getSelectedKey();
        if (selected == null) {
            bitsandbalance$clearPendingModifiers();
            return;
        }

        if (bitsandbalance$pendingModifierMask == 0) {
            return;
        }

        bitsandbalance$heldModifierMask = bitsandbalance$effectiveModifierMask(0) & bitsandbalance$pendingModifierMask;
        if (bitsandbalance$heldModifierMask != 0) {
            return;
        }

        bitsandbalance$finalizeModifierOnlyBind(selected);
    }

    @Inject(method = "keyPressed(III)Z", at = @At("RETURN"), require = 0)
    private void bitsandbalance$applyModifierStoreAfterControllingBind(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
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
        bitsandbalance$forceKeybindListRefresh();
    }

    @Inject(
        method = "keyPressed(III)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;resetMappingAndUpdateButtons()V",
            shift = At.Shift.BEFORE
        ),
        require = 0
    )
    private void bitsandbalance$applyModifierStoreBeforeUiRefresh_Controlling(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
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
    private void bitsandbalance$finalizeModifierOnlyBind(KeyMapping target) {
        if (target == null || bitsandbalance$lastModifierKey == InputConstants.UNKNOWN || bitsandbalance$lastModifierBit == 0) {
            bitsandbalance$clearPendingModifiers();
            return;
        }

        target.setKey(bitsandbalance$lastModifierKey);
        KeybindModifierStore.setRequiredMask(target, bitsandbalance$pendingModifierMask & ~bitsandbalance$lastModifierBit);
        KeyMapping.resetMapping();
        ((KeyBindsScreenAccessor) (Object) this).bitsandbalance$setSelectedKey(null);
        bitsandbalance$forceKeybindListRefresh();
        bitsandbalance$clearPendingModifiers();
    }

    @Unique
    private void bitsandbalance$clearPendingModifiers() {
        bitsandbalance$pendingModifierMask = 0;
        bitsandbalance$heldModifierMask = 0;
        bitsandbalance$lastModifierBit = 0;
        bitsandbalance$lastModifierKey = InputConstants.UNKNOWN;
    }

    @Unique
    private static int bitsandbalance$effectiveModifierMask(int modifiers) {
        try {
            if (modifiers != 0) {
                return modifiers;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                return 0;
            }

            long window = mc.getWindow().getWindow();
            boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
            boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

            int mask = 0;
            if (shiftDown) {
                mask |= GLFW.GLFW_MOD_SHIFT;
            }
            if (altDown) {
                mask |= GLFW.GLFW_MOD_ALT;
            }
            if (ctrlDown) {
                mask |= GLFW.GLFW_MOD_CONTROL;
            }
            return mask;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Unique
    private static int bitsandbalance$modifierBitForKey(int keyCode, int scanCode) {
        if (keyCode != -1) {
            return switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
                case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
                case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
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
                case 42, 54 -> GLFW.GLFW_MOD_SHIFT;
                case 56, 312 -> GLFW.GLFW_MOD_ALT;
                case 29, 285 -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        if (scanCode != -1) {
            return switch (scanCode) {
                case 42, 54 -> GLFW.GLFW_MOD_SHIFT;
                case 56, 312 -> GLFW.GLFW_MOD_ALT;
                case 29, 285 -> GLFW.GLFW_MOD_CONTROL;
                default -> 0;
            };
        }

        return 0;
    }
}
