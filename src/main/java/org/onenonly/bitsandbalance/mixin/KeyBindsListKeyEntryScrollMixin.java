package org.onenonly.bitsandbalance.mixin;

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? else
import net.minecraft.client.gui.GuiGraphics;
//? endif
import org.onenonly.bitsandbalance.client.KeybindAutoScrollSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class KeyBindsListKeyEntryScrollMixin {
    //? if mc == 1.21.1
    @Inject(method = "render", at = @At("HEAD"))
    private void bitsandbalance$enterFastKeybindScroll(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? else
    //? if mc == 1.21.11
    @Inject(method = "renderContent", at = @At("HEAD"))
    private void bitsandbalance$enterFastKeybindScroll(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? else
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void bitsandbalance$enterFastKeybindScroll(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? endif
    //? endif
        KeybindAutoScrollSpeed.enterKeybindRender();
    }

    //? if mc == 1.21.1
    @Inject(method = "render", at = @At("RETURN"))
    private void bitsandbalance$exitFastKeybindScroll(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? else
    //? if mc == 1.21.11
    @Inject(method = "renderContent", at = @At("RETURN"))
    private void bitsandbalance$exitFastKeybindScroll(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? else
    @Inject(method = "extractContent", at = @At("RETURN"))
    private void bitsandbalance$exitFastKeybindScroll(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTick, CallbackInfo ci) {
    //? endif
    //? endif
        KeybindAutoScrollSpeed.exitKeybindRender();
    }
}
