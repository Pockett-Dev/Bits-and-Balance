package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.client.ItemShareClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * QUARK EXACT IMPLEMENTATION: Force translucent render type for block items when alphaValue != 1.0F.
 */
@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    @Inject(
		method = "getRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        at = @At("HEAD"),
        cancellable = true,
		require = 0
    )
    private static void bitsandbalance$overrideRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
        // Force a blended render type so intermediate alpha values fade correctly.
        if (ItemShareClient.isRenderingWithAlpha() && ItemShareClient.alphaValue < 1.0F) {
            cir.setReturnValue(Sheets.translucentItemSheet());
        }
    }
}
