package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$resetItemShareAlphaAtChatRenderStart(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            int tickCount,
            int mouseX,
            int mouseY,
            Object displayMode,
            boolean changeCursorOnInsertions,
            CallbackInfo ci
    ) {
        ItemShareClient.endAlphaRender();
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("RETURN"),
            require = 1
    )
    private void bitsandbalance$resetItemShareAlphaAtChatRenderEnd(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            int tickCount,
            int mouseX,
            int mouseY,
            Object displayMode,
            boolean changeCursorOnInsertions,
            CallbackInfo ci
    ) {
        ItemShareClient.endAlphaRender();
    }
}
