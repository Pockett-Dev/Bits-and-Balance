package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures the item-share alpha context stays valid for the entire chat render.
 */
@Mixin(ChatComponent.class)
public class ChatComponentItemShareAlphaScopeMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$resetItemShareAlphaAtChatRenderStart(
            GuiGraphics guiGraphics,
            Font font,
            int tickCount,
            int mouseX,
            int mouseY,
            boolean focused,
            boolean bl,
            CallbackInfo ci
    ) {
        ItemShareClient.endAlphaRender();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
            at = @At("RETURN"),
            require = 1
    )
    private void bitsandbalance$resetItemShareAlphaAtChatRenderEnd(
            GuiGraphics guiGraphics,
            Font font,
            int tickCount,
            int mouseX,
            int mouseY,
            boolean focused,
            boolean bl,
            CallbackInfo ci
    ) {
        ItemShareClient.endAlphaRender();
    }
}
