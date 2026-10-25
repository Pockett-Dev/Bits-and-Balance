package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.onenonly.bitsandbalance.client.KeybindAutoScrollSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public class AbstractWidgetKeybindScrollMixin {
    @Inject(
            method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void bitsandbalance$renderLongKeybindTextWithShorterEndHolds(GuiGraphics guiGraphics, Font font, Component message, int centerX, int left, int top, int right, int bottom, int color, CallbackInfo ci) {
        if (KeybindAutoScrollSpeed.renderScrollingString(guiGraphics, font, message, centerX, left, top, right, bottom, color)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "renderScrollingString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIIII)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D")
    )
    private static double bitsandbalance$adjustLongKeybindScrollPeriod(double overflowPeriod, double minimumPeriod) {
        return KeybindAutoScrollSpeed.scrollPeriod(overflowPeriod, minimumPeriod);
    }
}