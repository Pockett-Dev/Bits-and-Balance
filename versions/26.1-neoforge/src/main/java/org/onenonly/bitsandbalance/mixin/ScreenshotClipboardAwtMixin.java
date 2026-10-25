package org.onenonly.bitsandbalance.mixin;

import org.onenonly.bitsandbalance.common.client.ScreenshotClipboardClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.main.Main")
public abstract class ScreenshotClipboardAwtMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void bitsandbalance$initializeClipboardSupport(String[] args, CallbackInfo ci) {
        ScreenshotClipboardClient.initializeClipboardSupport();
    }
}