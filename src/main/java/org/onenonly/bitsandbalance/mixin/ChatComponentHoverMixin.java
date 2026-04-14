package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adjusts hover detection coordinates to account for chat head offset.
 * Without this, vanilla thinks the username is where it would be without the chat head,
 * causing hover tooltips to appear in the wrong place.
 */
@Mixin(ChatComponent.class)
public class ChatComponentHoverMixin {
    
    /**
     * Modify the mouseX parameter to account for chat head offset.
     * This shifts the hover detection area to match where the text actually is.
     */
    @ModifyVariable(
        method = "getClickedComponentStyleAt",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private double adjustHoverX(double mouseX) {
        // Check if chat heads are enabled
        if (!org.onenonly.bitsandbalance.ClientConfig.enableChatHeads) {
            return mouseX;
        }
        
        // Calculate the offset we applied to text rendering
        int chatHeadSize = org.onenonly.bitsandbalance.ClientConfig.chatHeadSize;
        int chatHeadOffset = org.onenonly.bitsandbalance.ClientConfig.chatHeadOffset;
        int textOffsetX = chatHeadSize + chatHeadOffset + 2; // +2 for spacing (matches ChatComponentMixin)
        
        // Subtract the offset from mouseX to compensate for text being shifted right
        // This makes vanilla's hover detection work correctly
        return mouseX - textOffsetX;
    }
}

