package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
//? if mc != 26.2 && mc != 26.3
import net.minecraft.client.KeyboardHandler;
//? endif
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.common.client.ScreenshotClipboardClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

//? if mc == 26.2 || mc == 26.3
@Mixin(Screenshot.class)
//? else
@Mixin(KeyboardHandler.class)
//? endif
public class ScreenshotClipboardMixin {
    //? if mc == 26.2 || mc == 26.3
    @Redirect(
            method = "grab(Lnet/minecraft/client/Minecraft;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"
            ),
            require = 1
    )
    //? else
    @Redirect(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"
            ),
            require = 0
    )
    //? endif
    //? if mc == 26.2 || mc == 26.3
    private static void bitsandbalance$wrapScreenshotCall(File gameDirectory, RenderTarget renderTarget, Consumer<Component> consumer) {
    //? else
    private void bitsandbalance$wrapScreenshotCall(File gameDirectory, RenderTarget renderTarget, Consumer<Component> consumer) {
    //? endif
        if (!FabricClientConfig.enableScreenshotsToClipboard) {
            Screenshot.grab(gameDirectory, renderTarget, consumer);
            return;
        }

        ScreenshotClipboardClient.captureScreenshot(gameDirectory, renderTarget, consumer);
    }
}
