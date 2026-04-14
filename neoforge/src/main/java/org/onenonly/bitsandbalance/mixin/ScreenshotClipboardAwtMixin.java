package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.main.Main;
import org.onenonly.bitsandbalance.common.client.ScreenshotClipboardClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class ScreenshotClipboardAwtMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false, require = 0)
    private static void bitsandbalance$initializeScreenshotClipboardAwt(String[] args, CallbackInfo ci) {
        ScreenshotClipboardClient.initializeClipboardSupport();
    }
}