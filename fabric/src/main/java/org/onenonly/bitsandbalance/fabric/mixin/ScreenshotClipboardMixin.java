package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.common.client.ScreenshotClipboardClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardHandler.class)
public class ScreenshotClipboardMixin {
    @Redirect(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"
            ),
            require = 0
    )
    private void bitsandbalance$wrapScreenshotCall(File gameDirectory, RenderTarget renderTarget, Consumer<Component> consumer) {
        if (!FabricClientConfig.enableScreenshotsToClipboard) {
            Screenshot.grab(gameDirectory, renderTarget, consumer);
            return;
        }

        ScreenshotClipboardClient.captureScreenshot(gameDirectory, renderTarget, consumer);
    }
}