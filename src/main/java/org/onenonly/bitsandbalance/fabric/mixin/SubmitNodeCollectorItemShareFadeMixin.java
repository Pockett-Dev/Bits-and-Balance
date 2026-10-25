package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Fabric fix for 3D item fading:
 *
 * When a shared item renders as 3D geometry (block-items, glint, etc), the atlas render must use a
 * blended RenderType. If an opaque RenderType is used, the pipeline often outputs alpha=1.0,
 * so the final atlas blit can't fade the icon.
 *
 * We patch the RenderType at the submission point (SubmitNodeCollector), which avoids conflicts with
 * renderer mods (e.g. Indigo) that may already redirect the upstream call sites.
 */
@Mixin(OrderedSubmitNodeCollector.class)
public interface SubmitNodeCollectorItemShareFadeMixin {

    @ModifyVariable(
            method = "submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 7,
            require = 0
    )
    private static RenderType bitsandbalance$forceTranslucentItemSheetForChatFade(RenderType original) {
        if (!ItemShareClient.isRenderingWithAlpha()) return original;
        if (ItemShareClient.alphaValue >= 1.0F) return original;
        return Sheets.translucentItemSheet();
    }
}
