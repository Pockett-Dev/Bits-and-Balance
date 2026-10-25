package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.client.KeybindModifierStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsScreen.class)
public class KeyBindsScreenMultiModifierMixin {
    @Shadow
    public KeyMapping selectedKey;

    @Shadow
    private KeyBindsList keyBindsList;

    @Unique
    private KeyMapping bitsandbalance$selectedBeforeRelease;

    @Unique
    private KeyMapping bitsandbalance$selectedBeforeMouseClick;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$capturePressedModifiers(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.selectedKey == null) {
            return;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.isEscape()) {
            KeybindModifierStore.clearCapturedBinding(this);
            this.bitsandbalance$clearPressedState();
            return;
        }

        InputConstants.Key pressedKey = InputConstants.getKey(event);
        KeybindModifierStore.captureKeyPress(this, this.selectedKey, pressedKey, event.modifiers());

        if (!KeybindModifierStore.isModifierKey(pressedKey)) {
            KeyModifier modifier = KeybindModifierStore.primaryModifierForCapturedBinding(this, this.selectedKey, pressedKey);
            this.selectedKey.setKeyModifierAndCode(modifier, pressedKey);
            this.selectedKey.setKey(pressedKey);
            KeybindModifierStore.applyCapturedBinding(this, this.selectedKey);
            this.selectedKey = null;
            this.bitsandbalance$clearPressedState();
            this.keyBindsList.resetMappingAndUpdateButtons();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyReleased", at = @At("HEAD"))
    private void bitsandbalance$rememberSelectedKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        this.bitsandbalance$selectedBeforeRelease = this.selectedKey;
    }

    @Inject(method = "keyReleased", at = @At("RETURN"))
    private void bitsandbalance$applyCapturedModifiers(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.bitsandbalance$selectedBeforeRelease != null && this.selectedKey == null) {
            KeybindModifierStore.applyCapturedBinding(this, this.bitsandbalance$selectedBeforeRelease);
            this.keyBindsList.resetMappingAndUpdateButtons();
        }
        this.bitsandbalance$selectedBeforeRelease = null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void bitsandbalance$rememberMouseSelectedKey(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        this.bitsandbalance$selectedBeforeMouseClick = this.selectedKey;
        if (this.selectedKey != null) {
            KeybindModifierStore.captureCurrentModifiers(this, this.selectedKey);
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void bitsandbalance$applyMouseModifiers(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.bitsandbalance$selectedBeforeMouseClick != null && this.selectedKey == null) {
            KeybindModifierStore.applyCapturedBinding(this, this.bitsandbalance$selectedBeforeMouseClick);
            this.keyBindsList.resetMappingAndUpdateButtons();
        }
        this.bitsandbalance$selectedBeforeMouseClick = null;
    }

    @Unique
    private void bitsandbalance$clearPressedState() {
        if ((Object) this instanceof KeyBindsScreenModifierStateAccessor access) {
            access.bitsandbalance$setLastPressedKey(InputConstants.UNKNOWN);
            access.bitsandbalance$setLastPressedModifier(InputConstants.UNKNOWN);
            access.bitsandbalance$setIsLastKeyHeldDown(false);
            access.bitsandbalance$setIsLastModifierHeldDown(false);
        }
    }
}
