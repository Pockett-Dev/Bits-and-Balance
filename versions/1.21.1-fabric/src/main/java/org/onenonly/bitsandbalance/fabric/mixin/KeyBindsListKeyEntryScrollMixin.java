package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.onenonly.bitsandbalance.client.KeybindAutoScrollSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class KeyBindsListKeyEntryScrollMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void bitsandbalance$enterFastKeybindScroll(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        KeybindAutoScrollSpeed.enterKeybindRender();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bitsandbalance$exitFastKeybindScroll(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        KeybindAutoScrollSpeed.exitKeybindRender();
    }
}