package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/** Routes every shared-item quad through the blended item phase while chat fading is active. */
@Mixin(SubmitNodeCollection.class)
public abstract class SubmitNodeCollectorItemShareFadeMixin {
    @ModifyVariable(
            method = "submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILnet/minecraft/client/resources/model/geometry/ItemQuads;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 7,
            require = 1
    )
    private ItemQuads bitsandbalance$forceTranslucentItemQuadsForChatFade(ItemQuads quads) {
        if (!ItemShareClient.isRenderingWithAlpha() || ItemShareClient.alphaValue >= 1.0F) {
            return quads;
        }
        return new ItemQuads(quads.all(), List.of(), quads.all());
    }
}
