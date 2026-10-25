package org.onenonly.bitsandbalance.fabric.mixin;

import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.onenonly.bitsandbalance.client.ChatHeadPixelAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.font.PlayerGlyphProvider$1$1")
public abstract class PlayerGlyphProviderChatHeadOffsetMixin {
    @ModifyVariable(method = "createGlyph", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float bitsandbalance$alignChatHeadX(float x) {
        return x + FabricClientConfig.chatHeadOffset;
    }

    @ModifyVariable(method = "createGlyph", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float bitsandbalance$alignChatHeadY(float y) {
        return y + ChatHeadPixelAlignment.baselineOffsetY() + FabricClientConfig.chatHeadOffsetY;
    }
}
