package org.onenonly.bitsandbalance.fabric.mixin;

import java.util.function.Consumer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.onenonly.bitsandbalance.fabric.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ensures per-item chat fade alpha is active during the *actual* deferred item render stage.
 *
 * In 1.21.10+, GuiGraphics#renderItem only submits a GuiItemRenderState; the 3D model render
 * happens later when GuiRenderer traverses GuiRenderState items.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererItemShareAlphaContextMixin {
    @Redirect(
            method = {"prepareItemElements", "prepare"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V"
            ),
            require = 0
    )
    private void bitsandbalance$wrapForEachItemWithAlphaContext(GuiRenderState renderState, Consumer<GuiItemRenderState> action) {
        renderState.forEachItem(guiItem -> {
            float alpha = 1.0F;
            boolean hasAlpha = false;

            if ((Object) guiItem instanceof ItemShareGuiItemAlpha alphaCarrier && alphaCarrier.bitsandbalance$hasItemShareAlpha()) {
                alpha = alphaCarrier.bitsandbalance$getItemShareAlpha();
                hasAlpha = alpha < 1.0F;
            }

            if (hasAlpha) {
                ItemShareClient.beginAlphaRender(alpha);
            }

            try {
                action.accept(guiItem);
            } finally {
                if (hasAlpha) {
                    ItemShareClient.endAlphaRender();
                }
            }
        });
    }
}
