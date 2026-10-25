package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.onenonly.bitsandbalance.client.KeybindAutoScrollSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class KeyBindsListKeyEntryScrollMixin {
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void bitsandbalance$enterFastKeybindScroll(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        KeybindAutoScrollSpeed.enterKeybindRender();
    }

    @Inject(method = "extractContent", at = @At("RETURN"))
    private void bitsandbalance$exitFastKeybindScroll(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
        KeybindAutoScrollSpeed.exitKeybindRender();
    }
}