package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.onenonly.bitsandbalance.fabric.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tags GUI item render submissions with the current chat fade alpha.
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsItemShareMixin {
    private static GuiItemRenderState bitsandbalance$tagIfNeeded(GuiItemRenderState renderState) {
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
            ((ItemShareGuiItemAlpha) (Object) renderState).bitsandbalance$setItemShareAlpha(ItemShareClient.alphaValue);
        }
        return renderState;
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"
            ),
            index = 0,
            require = 1
    )
    private GuiItemRenderState bitsandbalance$tagItemShareAlpha(GuiItemRenderState renderState) {
        return bitsandbalance$tagIfNeeded(renderState);
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"
            ),
            index = 0,
            require = 0
    )
    private GuiItemRenderState bitsandbalance$tagItemShareAlphaNoContext(GuiItemRenderState renderState) {
        return bitsandbalance$tagIfNeeded(renderState);
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"
            ),
            index = 0,
            require = 0
    )
    private GuiItemRenderState bitsandbalance$tagItemShareAlphaNoContextZ(GuiItemRenderState renderState) {
        return bitsandbalance$tagIfNeeded(renderState);
    }
}
