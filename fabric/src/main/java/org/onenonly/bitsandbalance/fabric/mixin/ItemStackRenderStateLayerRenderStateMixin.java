package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

/**
 * When rendering inline chat items, we need the item geometry to be submitted with a blended (translucent)
 * render type; otherwise alpha changes won't visually fade.
 */
@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState", priority = 900)
public class ItemStackRenderStateLayerRenderStateMixin {

    @Redirect(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"
            ),
            require = 0
    )
    @SuppressWarnings("null")
    private void bitsandbalance$submitItemWithTranslucentFade(
            SubmitNodeCollector collector,
            PoseStack poseStack,
            ItemDisplayContext displayContext,
            int packedLight,
            int packedOverlay,
            int depth,
            int[] tintLayers,
            List<BakedQuad> quads,
            RenderType renderType,
            ItemStackRenderState.FoilType foilType
    ) {
        RenderType effectiveType = renderType;
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
            effectiveType = Sheets.translucentItemSheet();
        }
        collector.submitItem(poseStack, displayContext, packedLight, packedOverlay, depth, tintLayers, quads, effectiveType, foilType);
    }
}
