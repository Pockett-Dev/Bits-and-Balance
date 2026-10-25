package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.onenonly.bitsandbalance.fabric.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tags GUI item render submissions with the current chat fade alpha.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsItemShareMixin {
    private static GuiItemRenderState bitsandbalance$tagIfNeeded(GuiItemRenderState renderState) {
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
            ((ItemShareGuiItemAlpha) (Object) renderState).bitsandbalance$setItemShareAlpha(ItemShareClient.alphaValue);
        }
        return renderState;
    }

    @ModifyArg(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
            ),
            index = 0,
            require = 1
    )
    private GuiItemRenderState bitsandbalance$tagItemShareAlpha(GuiItemRenderState renderState) {
        return bitsandbalance$tagIfNeeded(renderState);
    }
}
