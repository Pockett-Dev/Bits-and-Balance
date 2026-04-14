package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphicsExtractor$RenderingTextCollector")
public class RenderingTextCollectorItemShareMixin {
    @Shadow
    @Final
    private GuiGraphicsExtractor this$0;

    @Inject(method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V", at = @At("HEAD"), require = 0)
    private void bitsandbalance$renderSharedItemInline(TextAlignment alignment, int x, int y,
                                                       ActiveTextCollector.Parameters parameters,
                                                       FormattedCharSequence sequence,
                                                       CallbackInfo ci) {
        float opacity = parameters.opacity();
        ItemShareClient.beginChatLineAlpha(opacity);
        try {
            ItemShareClient.renderItemForMessage(this.this$0, sequence, (float) x, (float) y, ARGB.white(opacity));
        } finally {
            ItemShareClient.endChatLineAlpha();
        }
    }
}